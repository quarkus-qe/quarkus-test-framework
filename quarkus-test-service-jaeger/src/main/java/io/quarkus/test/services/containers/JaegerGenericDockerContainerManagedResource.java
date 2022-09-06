package io.quarkus.test.services.containers;

import static io.quarkus.test.bootstrap.JaegerService.JAEGER_TRACE_URL_PROPERTY;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.utils.DockerUtils;

public class JaegerGenericDockerContainerManagedResource extends GenericDockerContainerManagedResource {

    private static final String COLLECTOR_OTLP_ENABLED = "COLLECTOR_OTLP_ENABLED";
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
        container.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        if (model.shouldUseOtlpCollector()) {
            container.addEnv(COLLECTOR_OTLP_ENABLED, "true");
        }

        return container;
    }

    private String getJaegerTraceUrl() {
        return getURI(Protocol.HTTP)
                .withPort(getMappedPort(model.getTracePort()))
                .toString();
    }
}
