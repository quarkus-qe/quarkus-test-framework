package io.quarkus.test.services.quarkus;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.QuarkusCliClient;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.logging.FileQuarkusApplicationLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.ProcessUtils;
import io.quarkus.test.utils.SocketUtils;

public class CliDevModeLocalhostQuarkusApplicationManagedResource extends QuarkusManagedResource {

    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final String QUARKUS_PLATFORM_GROUP_ID = "quarkus.platform.group-id";
    private static final String QUARKUS_PLATFORM_GROUP_ID_VALUE = "io.quarkus";
    private static final String QUARKUS_PLATFORM_ARTIFACT_ID = "quarkus.platform.artifact-id";
    private static final String QUARKUS_PLATFORM_ARTIFACT_ID_VALUE = "quarkus-bom";
    private static final String QUARKUS_PLATFORM_VERSION = "quarkus.platform.version";

    private final ServiceContext serviceContext;
    private final QuarkusCliClient client;

    private Process process;
    private LoggingHandler loggingHandler;
    private int assignedHttpPort;

    public CliDevModeLocalhostQuarkusApplicationManagedResource(ServiceContext serviceContext,
            QuarkusCliClient client) {
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
            process = client.runOnDev(serviceContext.getServiceFolder(), getPropertiesForCommand());

            File logFile = serviceContext.getServiceFolder().resolve(QuarkusCliClient.LOG_FILE).toFile();
            loggingHandler = new FileQuarkusApplicationLoggingHandler(serviceContext, logFile);
            loggingHandler.startWatching();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (loggingHandler != null) {
            loggingHandler.stopWatching();
        }

        ProcessUtils.destroy(process);
    }

    @Override
    public String getHost(Protocol protocol) {
        return "http://localhost";
    }

    @Override
    public int getPort(Protocol protocol) {
        return assignedHttpPort;
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
    protected LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    private Map<String, String> getPropertiesForCommand() {
        Map<String, String> runtimeProperties = new HashMap<>(serviceContext.getOwner().getProperties());
        runtimeProperties.putIfAbsent(QUARKUS_HTTP_PORT_PROPERTY, "" + assignedHttpPort);
        runtimeProperties.putIfAbsent(QUARKUS_PLATFORM_GROUP_ID, QUARKUS_PLATFORM_GROUP_ID_VALUE);
        runtimeProperties.putIfAbsent(QUARKUS_PLATFORM_ARTIFACT_ID, QUARKUS_PLATFORM_ARTIFACT_ID_VALUE);
        runtimeProperties.putIfAbsent(QUARKUS_PLATFORM_VERSION, io.quarkus.builder.Version.getVersion());

        return runtimeProperties;
    }

    private void assignPorts() {
        assignedHttpPort = getOrAssignPortByProperty(QUARKUS_HTTP_PORT_PROPERTY);
    }

    private int getOrAssignPortByProperty(String property) {
        String port = serviceContext.getOwner().getProperties().get(property);
        if (StringUtils.isEmpty(port)) {
            return SocketUtils.findAvailablePort();
        }

        return Integer.parseInt(port);
    }
}
