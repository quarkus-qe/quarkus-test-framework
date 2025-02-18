package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.GrafanaContainer;

public class GrafanaContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    private final ServiceLoader<GrafanaContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(GrafanaContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    // webUiPort
    private int port;
    private int restPort;
    private int otlpGrpcPort;
    private String expectedLog;

    @Override
    protected String getImage() {
        return image;
    }

    @Override
    protected Integer getPort() {
        return port;
    }

    @Override
    protected ServiceContext getContext() {
        return context;
    }

    @Override
    protected String getExpectedLog() {
        return expectedLog;
    }

    protected int getOtlpGrpcPort() {
        return otlpGrpcPort;
    }

    protected int getRestPort() {
        return restPort;
    }

    @Override
    public void init(Annotation annotation) {
        GrafanaContainer metadata = (GrafanaContainer) annotation;
        this.image = metadata.image();
        this.port = metadata.webUIPort();
        this.restPort = metadata.restPort();
        this.otlpGrpcPort = metadata.otlpGrpcPort();
        this.expectedLog = metadata.expectedLog();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        for (GrafanaContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        return new GrafanaGenericDockerContainerManagedResource(this);
    }
}
