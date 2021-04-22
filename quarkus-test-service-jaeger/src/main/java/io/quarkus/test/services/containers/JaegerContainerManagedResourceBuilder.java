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
    private int restPort;
    private int tracePort;
    private String expectedLog;

    @Override
    protected String getImage() {
        return image;
    }

    @Override
    protected Integer getPort() {
        return restPort;
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

    @Override
    public void init(Annotation annotation) {
        JaegerContainer metadata = (JaegerContainer) annotation;
        this.image = metadata.image();
        this.restPort = metadata.restPort();
        this.tracePort = metadata.tracePort();
        this.expectedLog = metadata.expectedLog();
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
