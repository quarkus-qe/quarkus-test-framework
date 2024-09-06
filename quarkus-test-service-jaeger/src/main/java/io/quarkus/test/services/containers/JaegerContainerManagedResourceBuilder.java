package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.JaegerContainer;

public class JaegerContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    private final ServiceLoader<JaegerContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(JaegerContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    /**
     * Port used to collect traces. Depending on the {@link JaegerContainer#useOtlpCollector()}, value is either
     * {@link JaegerContainer#otlpPort()} or {@link JaegerContainer#restPort()}.
     */
    private int port;
    private int tracePort;
    private String expectedLog;
    private boolean useOtlpCollector;
    private boolean tlsEnabled;

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

    protected int getTracePort() {
        return tracePort;
    }

    protected boolean shouldUseOtlpCollector() {
        return useOtlpCollector;
    }

    protected boolean isTlsEnabled() {
        return tlsEnabled;
    }

    @Override
    public void init(Annotation annotation) {
        JaegerContainer metadata = (JaegerContainer) annotation;
        this.image = metadata.image();
        this.port = metadata.useOtlpCollector() ? metadata.otlpPort() : metadata.restPort();
        this.tracePort = metadata.tracePort();
        this.expectedLog = metadata.expectedLog();
        this.useOtlpCollector = metadata.useOtlpCollector();
        this.tlsEnabled = metadata.tls();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        for (JaegerContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        return new JaegerGenericDockerContainerManagedResource(this);
    }
}
