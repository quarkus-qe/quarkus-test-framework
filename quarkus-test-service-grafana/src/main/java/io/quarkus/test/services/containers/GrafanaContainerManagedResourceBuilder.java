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
    private int lokiPort;
    private int tempoPort;
    private int prometheusPort;
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

    protected int getLokiPort() {
        return lokiPort;
    }

    protected int getTempoPort() {
        return tempoPort;
    }

    protected int getPrometheusPort() {
        return prometheusPort;
    }

    @Override
    public void init(Annotation annotation) {
        GrafanaContainer metadata = (GrafanaContainer) annotation;
        this.image = metadata.image();
        this.port = metadata.webUIPort();
        this.lokiPort = metadata.lokiPort();
        this.tempoPort = metadata.tempoPort();
        this.prometheusPort = metadata.prometheusPort();
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
