package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.Container;
import io.quarkus.test.utils.PropertiesUtils;

public class ContainerManagedResourceBuilder implements ManagedResourceBuilder {

    private final ServiceLoader<ContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(ContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    private String expectedLog;
    private String[] command;
    private Integer port;

    protected String getImage() {
        return image;
    }

    protected String getExpectedLog() {
        return expectedLog;
    }

    protected String[] getCommand() {
        return Optional.ofNullable(command).orElse(new String[] {});
    }

    protected Integer getPort() {
        return port;
    }

    protected ServiceContext getContext() {
        return context;
    }

    @Override
    public void init(Annotation annotation) {
        Container metadata = (Container) annotation;
        this.image = PropertiesUtils.resolveProperty(metadata.image());
        this.command = metadata.command();
        this.expectedLog = PropertiesUtils.resolveProperty(metadata.expectedLog());
        this.port = metadata.port();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        for (ContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(context)) {
                return binding.init(this);
            }
        }

        return new GenericDockerContainerManagedResource(this);
    }
}
