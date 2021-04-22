package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.JaegerService.JAEGER_TRACE_URL_PROPERTY;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.logging.TestContainersLoggingHandler;

public abstract class BaseJaegerContainerManagedResource extends DockerContainerManagedResource {
    protected final JaegerContainerManagedResourceBuilder model;

    private GenericContainer<?> jaegerContainer;
    private TestContainersLoggingHandler jaegerLoggingHandler;

    protected BaseJaegerContainerManagedResource(JaegerContainerManagedResourceBuilder model) {
        super(model.getContext());
        this.model = model;
    }

    protected abstract int getTargetPort();

    protected abstract int getTraceTargetPort();

    protected abstract GenericContainer<?> initJaegerContainer();

    protected abstract GenericContainer<?> initJaegerContainer(GenericContainer<?> jaeger);

    @Override
    protected GenericContainer<?> initContainer() {
        jaegerContainer = initJaegerContainer();
        jaegerLoggingHandler = new TestContainersLoggingHandler(model.getContext(), jaegerContainer);

        return jaegerContainer;
    }

    @Override
    public void start() {
        super.start();
        model.getContext().put(JAEGER_TRACE_URL_PROPERTY, getJaegerTraceUrl());
    }

    @Override
    public void stop() {
        jaegerLoggingHandler.stopWatching();
        super.stop();
    }

    @Override
    public boolean isRunning() {
        return super.isRunning();
    }

    protected String getJaegerVersion() {
        return StringUtils.defaultIfBlank(model.getVersion(), "latest");
    }

    protected String getJaegerTraceUrl() {
        return "http://" + jaegerContainer.getHost() + ":" + jaegerContainer.getMappedPort(getTraceTargetPort());
    }

    private boolean isRegistryRunning() {
        return jaegerContainer != null && jaegerContainer.isRunning();
    }
}
