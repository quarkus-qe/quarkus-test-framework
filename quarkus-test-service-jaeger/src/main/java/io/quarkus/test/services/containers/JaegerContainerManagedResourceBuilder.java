package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.JaegerContainer;

public class JaegerContainerManagedResourceBuilder implements ManagedResourceBuilder {

    private final ServiceLoader<JaegerContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(JaegerContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String version;

    protected String getVersion() {
        return version;
    }

    protected ServiceContext getContext() {
        return context;
    }

    @Override
    public void init(Annotation annotation) {
        JaegerContainer metadata = (JaegerContainer) annotation;
        this.version = metadata.version();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        for (JaegerContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        return new JaegerContainerManagedResource(this);
    }
}
