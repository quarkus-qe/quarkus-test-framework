package io.quarkus.qe.resources;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class JaegerContainer extends GenericContainer<JaegerContainer> {
    public static final int REST_PORT = 16686;
    private static final int TRACE_PORT = 4318;
    private static final int STARTUP_TIMEOUT_SECONDS = 30;

    public JaegerContainer() {
        super("quay.io/jaegertracing/all-in-one:1.36");
        addEnv("COLLECTOR_OTLP_ENABLED", "true");
        waitingFor(Wait.forLogMessage(".*server started.*", 1));
        withStartupTimeout(Duration.ofSeconds(STARTUP_TIMEOUT_SECONDS));
        addFixedExposedPort(REST_PORT, REST_PORT);
        addFixedExposedPort(TRACE_PORT, TRACE_PORT);
    }
}
