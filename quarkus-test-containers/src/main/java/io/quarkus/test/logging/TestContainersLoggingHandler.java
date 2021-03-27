package io.quarkus.test.logging;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.ServiceContext;

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
        onStringDifference(newLogs, oldLogs);
        oldLogs = newLogs;
    }

}
