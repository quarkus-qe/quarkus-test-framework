package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.Container;

public class ContainerManagedResourceBuilder implements ManagedResourceBuilder {

    private final ServiceLoader<ContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(ContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    private String expectedLog;
    private String command;
    private Integer port;

    public String getImage() {
        return image;
    }

    public String getExpectedLog() {
        return expectedLog;
    }

    public String getCommand() {
        return command;
    }

    public Integer getPort() {
        return port;
    }

    public ServiceContext getContext() {
        return context;
    }

    @Override
    public void init(Annotation annotation) {
        Container metadata = (Container) annotation;
        this.image = metadata.image();
        this.command = metadata.command();
        this.expectedLog = metadata.expectedLog();
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

        return new DockerContainerManagedResource(this);
    }
}
