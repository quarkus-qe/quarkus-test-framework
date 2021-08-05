package io.quarkus.test.logging;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.Service;

public class TestContainersLoggingHandler extends ServiceLoggingHandler {

    private final GenericContainer<?> container;

    private String oldLogs;

    public TestContainersLoggingHandler(Service service, GenericContainer<?> container) {
        super(service);
        this.container = container;
    }

    @Override
    protected synchronized void handle() {
        String newLogs = container.getLogs();
        onStringDifference(newLogs, oldLogs);
        oldLogs = newLogs;
    }

}
