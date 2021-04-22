package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.JaegerService.JAEGER_TRACE_URL_PROPERTY;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.Protocol;

public class JaegerGenericDockerContainerManagedResource extends GenericDockerContainerManagedResource {

    private final JaegerContainerManagedResourceBuilder model;

    protected JaegerGenericDockerContainerManagedResource(JaegerContainerManagedResourceBuilder model) {
        super(model);

        this.model = model;
    }

    @Override
    public void start() {
        super.start();
        model.getContext().put(JAEGER_TRACE_URL_PROPERTY, getJaegerTraceUrl());
    }

    @Override
    protected GenericContainer<?> initContainer() {
        GenericContainer<?> container = super.initContainer();
        container.addExposedPort(model.getTracePort());
        return container;
    }

    private String getJaegerTraceUrl() {
        return getHost(Protocol.HTTP) + ":" + getMappedPort(model.getTracePort());
    }

}
