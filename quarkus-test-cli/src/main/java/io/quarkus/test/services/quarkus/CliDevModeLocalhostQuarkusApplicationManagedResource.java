package io.quarkus.test.services.quarkus;

import static io.quarkus.test.services.quarkus.model.QuarkusProperties.PLATFORM_GROUP_ID;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.logging.FileServiceLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.scenarios.annotations.DisabledOnQuarkusSnapshotCondition;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.quarkus.model.LaunchMode;
import io.quarkus.test.services.quarkus.model.QuarkusProperties;
import io.quarkus.test.utils.FileUtils;
import io.quarkus.test.utils.ProcessUtils;
import io.quarkus.test.utils.SocketUtils;

public class CliDevModeLocalhostQuarkusApplicationManagedResource extends QuarkusManagedResource {

    protected static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    protected static final String QUARKUS_PLATFORM_ARTIFACT_ID = "quarkus.platform.artifact-id";
    protected static final String QUARKUS_PLATFORM_ARTIFACT_ID_VALUE = "quarkus-bom";
    protected static final String QUARKUS_PLATFORM_VERSION = "quarkus.platform.version";

    protected final ServiceContext serviceContext;
    protected final QuarkusCliClient client;

    protected int assignedHttpPort;
    private Process process;
    private LoggingHandler loggingHandler;

    public CliDevModeLocalhostQuarkusApplicationManagedResource(ServiceContext serviceContext,
            QuarkusCliClient client) {
        super(serviceContext);
        this.serviceContext = serviceContext;
        this.client = client;
    }

    @Override
    public void start() {
        if (process != null && process.isAlive()) {
            // do nothing
            return;
        }

        try {
            assignPorts();
            File logFile = serviceContext.getServiceFolder().resolve(QuarkusCliClient.DEV_MODE_LOG_FILE).toFile();
            process = client.runOnDev(serviceContext.getServiceFolder(), logFile, getPropertiesForCommand());
            loggingHandler = new FileServiceLoggingHandler(serviceContext.getOwner(), logFile);
            loggingHandler.startWatching();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
            File logFile = serviceContext.getServiceFolder().resolve(QuarkusCliClient.DEV_MODE_LOG_FILE).toFile();
            FileUtils.deleteFileContent(logFile);
        }

        ProcessUtils.destroy(process);
    }

    @Override
    public URILike getURI(Protocol protocol) {
        return createURI(protocol.getValue(), "localhost", assignedHttpPort);
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public boolean isRunning() {
        return process != null && process.isAlive() && super.isRunning();
    }

    @Override
    protected LaunchMode getLaunchMode() {
        return LaunchMode.DEV;
    }

    @Override
    protected LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    protected Map<String, String> getPropertiesForCommand() {
        Map<String, String> runtimeProperties = new HashMap<>(serviceContext.getOwner().getProperties());
        runtimeProperties.putIfAbsent(QUARKUS_HTTP_PORT_PROPERTY, "" + assignedHttpPort);
        runtimeProperties.putIfAbsent(QUARKUS_PLATFORM_VERSION, QuarkusProperties.getVersion());

        if (DisabledOnQuarkusSnapshotCondition.isQuarkusSnapshotVersion()) {
            // In Quarkus Snapshot (999-SNAPSHOT), we can't use the quarkus platform bom as it's not resolved,
            // so we need to overwrite it.
            runtimeProperties.putIfAbsent(PLATFORM_GROUP_ID.getPropertyKey(), PLATFORM_GROUP_ID.get());
            runtimeProperties.putIfAbsent(QUARKUS_PLATFORM_ARTIFACT_ID, QUARKUS_PLATFORM_ARTIFACT_ID_VALUE);
        }

        return runtimeProperties;
    }

    private void assignPorts() {
        assignedHttpPort = getOrAssignPortByProperty(QUARKUS_HTTP_PORT_PROPERTY);
    }

    private int getOrAssignPortByProperty(String property) {
        return serviceContext.getOwner().getProperty(property)
                .filter(str -> !str.isEmpty())
                .map(Integer::parseInt)
                .orElseGet(SocketUtils::findAvailablePort);
    }
}
