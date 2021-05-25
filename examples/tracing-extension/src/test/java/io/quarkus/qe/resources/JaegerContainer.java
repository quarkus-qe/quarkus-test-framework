package io.quarkus.qe.resources;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;

public class JaegerContainer extends GenericContainer<JaegerContainer> {
    public static final int REST_PORT = 16686;
    private static final int TRACE_PORT = 14268;
    private static final int THRIFT_PORT = 6831;
    private static final int STARTUP_TIMEOUT = 30000;

    public JaegerContainer() {
        super("jaegertracing/all-in-one:latest");
        withStartupTimeout(Duration.ofMillis(STARTUP_TIMEOUT));
        addFixedExposedPort(REST_PORT, REST_PORT);
        addFixedExposedPort(TRACE_PORT, TRACE_PORT);
        addFixedExposedPort(THRIFT_PORT, THRIFT_PORT);
    }
}
