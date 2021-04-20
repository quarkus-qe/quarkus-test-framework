package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.AmqContainer;

public class AmqContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    private final ServiceLoader<AmqContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(AmqContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    private String expectedLog;
    private Integer port;

    @Override
    protected String getImage() {
        return image;
    }

    @Override
    protected String getExpectedLog() {
        return expectedLog;
    }

    @Override
    protected Integer getPort() {
        return port;
    }

    @Override
    public ServiceContext getContext() {
        return context;
    }

    @Override
    public void init(Annotation annotation) {
        AmqContainer metadata = (AmqContainer) annotation;
        this.image = metadata.image();
        this.expectedLog = metadata.expectedLog();
        this.port = metadata.port();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        for (AmqContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        return new GenericDockerContainerManagedResource(this);
    }
}
