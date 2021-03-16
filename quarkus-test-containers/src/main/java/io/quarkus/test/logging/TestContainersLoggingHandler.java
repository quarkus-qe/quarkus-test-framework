package io.quarkus.test.logging;

import java.util.stream.Stream;

import org.apache.maven.shared.utils.StringUtils;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.ServiceContext;

public class TestContainersLoggingHandler extends LoggingHandler {

    private final GenericContainer<?> container;

    private String oldLogs;

    public TestContainersLoggingHandler(ServiceContext context, GenericContainer<?> container) {
        super(context);
        this.container = container;
    }

    @Override
    protected synchronized void handle() {
        String newLogs = container.getLogs();

        if (StringUtils.isNotEmpty(oldLogs)) {
            onLines(StringUtils.replace(newLogs, oldLogs, ""));
        } else {
            onLines(newLogs);
        }

        oldLogs = newLogs;
    }

    private void onLines(String lines) {
        Stream.of(lines.split("\\r?\\n")).forEach(this::onLine);
    }

}
