package io.quarkus.test.services.quarkus;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.logging.FileQuarkusApplicationLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.SocketUtils;

public abstract class LocalhostQuarkusApplicationManagedResource extends QuarkusManagedResource {

    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";
    private static final String QUARKUS_HTTP_SSL_PORT_PROPERTY = "quarkus.http.ssl-port";
    private static final String LOG_OUTPUT_FILE = "out.log";

    private final QuarkusApplicationManagedResourceBuilder model;

    private File logOutputFile;
    private Process process;
    private LoggingHandler loggingHandler;
    private int assignedHttpPort;
    private int assignedHttpsPort;

    public LocalhostQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        this.model = model;
        this.logOutputFile = new File(model.getContext().getServiceFolder().resolve(LOG_OUTPUT_FILE).toString());
    }

    protected abstract List<String> prepareCommand(List<String> systemArguments);

    @Override
    public void start() {
        if (process != null && process.isAlive()) {
            // do nothing
            return;
        }

        try {
            assignPorts();
            process = new ProcessBuilder(prepareCommand(getPropertiesForCommand()))
                    .redirectErrorStream(true)
                    .redirectOutput(logOutputFile)
                    .directory(model.getContext().getServiceFolder().toFile())
                    .start();

            loggingHandler = new FileQuarkusApplicationLoggingHandler(model.getContext(), logOutputFile);
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

        try {
            if (process != null) {
                process.destroy();
                process.waitFor();
            }
        } catch (InterruptedException ignored) {

        }
    }

    @Override
    public String getHost(Protocol protocol) {
        validateProtocol(protocol);
        return protocol.getValue() + "://localhost";
    }

    @Override
    public int getPort(Protocol protocol) {
        validateProtocol(protocol);
        if (protocol == Protocol.HTTPS) {
            return assignedHttpsPort;
        }

        return assignedHttpPort;
    }

    @Override
    public List<String> logs() {
        return loggingHandler.logs();
    }

    @Override
    public void restart() {
        stop();
        if (model.containsBuildProperties()) {
            model.build();
        }

        start();
    }

    @Override
    protected LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    private void assignPorts() {
        assignedHttpPort = getOrAssignPortByProperty(QUARKUS_HTTP_PORT_PROPERTY);
        if (model.isSslEnabled()) {
            assignedHttpsPort = getOrAssignPortByProperty(QUARKUS_HTTP_SSL_PORT_PROPERTY);
        }
    }

    private int getOrAssignPortByProperty(String property) {
        String port = model.getContext().getOwner().getProperties().get(property);
        if (StringUtils.isEmpty(port)) {
            return SocketUtils.findAvailablePort();
        }

        return Integer.parseInt(port);
    }

    private List<String> getPropertiesForCommand() {
        Map<String, String> runtimeProperties = new HashMap<>(model.getContext().getOwner().getProperties());
        runtimeProperties.putIfAbsent(QUARKUS_HTTP_PORT_PROPERTY, "" + assignedHttpPort);
        if (model.isSslEnabled()) {
            runtimeProperties.putIfAbsent(QUARKUS_HTTP_SSL_PORT_PROPERTY, "" + assignedHttpsPort);
        }

        return runtimeProperties.entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList());
    }

    private void validateProtocol(Protocol protocol) {
        if (protocol == Protocol.HTTPS && !model.isSslEnabled()) {
            fail("SSL was not enabled. Use: `@QuarkusApplication(ssl = true)`");
        }
    }

}
