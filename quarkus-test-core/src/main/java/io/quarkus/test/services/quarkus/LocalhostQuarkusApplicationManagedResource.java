package io.quarkus.test.services.quarkus;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.quarkus.test.logging.FileQuarkusApplicationLoggingHandler;
import io.quarkus.test.logging.LoggingHandler;
import io.quarkus.test.utils.SocketUtils;
import io.quarkus.utilities.JavaBinFinder;

public class LocalhostQuarkusApplicationManagedResource implements QuarkusManagedResource {

    private static final String EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED = "features";
    private static final String QUARKUS_HTTP_PORT_PROPERTY = "quarkus.http.port";

    private final QuarkusApplicationManagedResourceBuilder model;

    private Process process;
    private LoggingHandler loggingHandler;

    public LocalhostQuarkusApplicationManagedResource(QuarkusApplicationManagedResourceBuilder model) {
        this.model = model;
    }

    @Override
    public void start() {
        if (process != null && process.isAlive()) {
            // do nothing
            return;
        }

        try {
            assignPortIfNotSet();
            process = new ProcessBuilder(prepareCommand()).redirectErrorStream(true)
                    .directory(model.getArtifact().getParent().toFile()).start();

            loggingHandler = new FileQuarkusApplicationLoggingHandler(model.getContext(), "out.log", process.getInputStream());
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
    public String getHost() {
        return "http://localhost";
    }

    @Override
    public int getPort() {
        return Integer.valueOf(model.getContext().getOwner().getProperties().get(QUARKUS_HTTP_PORT_PROPERTY));
    }

    @Override
    public boolean isRunning() {
        return loggingHandler != null && loggingHandler.logsContains(EXPECTED_OUTPUT_FROM_SUCCESSFULLY_STARTED);
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

    private int assignPortIfNotSet() {
        String port = model.getContext().getOwner().getProperties().get(QUARKUS_HTTP_PORT_PROPERTY);
        if (StringUtils.isEmpty(port)) {
            int randomPort = SocketUtils.findAvailablePort();
            model.getContext().getOwner().withProperty(QUARKUS_HTTP_PORT_PROPERTY, "" + randomPort);
            return randomPort;
        }

        return Integer.parseInt(port);
    }

    private List<String> prepareCommand() {
        List<String> systemProperties = model.getContext().getOwner().getProperties().entrySet().stream()
                .map(e -> "-D" + e.getKey() + "=" + e.getValue()).collect(Collectors.toList());
        List<String> command = new LinkedList<>();
        if (model.getArtifact().getFileName().toString().endsWith(".jar")) {
            command.add(JavaBinFinder.findBin());
            command.addAll(systemProperties);
            command.add("-jar");
            command.add(model.getArtifact().toAbsolutePath().toString());
        } else {
            command.add(model.getArtifact().toAbsolutePath().toString());
            command.addAll(systemProperties);
        }

        return command;
    }

}
