package io.quarkus.test.services.containers;

import org.testcontainers.containers.GenericContainer;

public class JaegerContainerManagedResource extends BaseJaegerContainerManagedResource {

    private static final String JAEGER_IMAGE = "jaegertracing/all-in-one";
    private static final int REST_PORT = 14268;
    private static final int TRACE_PORT = 16686;

    protected JaegerContainerManagedResource(JaegerContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected int getTargetPort() {
        return REST_PORT;
    }

    @Override
    protected int getTraceTargetPort() {
        return TRACE_PORT;
    }

    @Override
    protected GenericContainer<?> initJaegerContainer() {
        return new GenericContainer<>(JAEGER_IMAGE + ":" + getJaegerVersion())
                .withExposedPorts(REST_PORT, TRACE_PORT);
    }

    @Override
    protected GenericContainer<?> initJaegerContainer(GenericContainer<?> jaegerContainer) {
        return jaegerContainer.withExposedPorts(REST_PORT, TRACE_PORT);
    }
}
