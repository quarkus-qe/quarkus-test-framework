package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.KeycloakContainer;
import io.quarkus.test.utils.PropertiesUtils;

public class KeycloakContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    private final ServiceLoader<KeycloakContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(KeycloakContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    private int restPort;
    private String[] command;
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
    protected String[] getCommand() {
        return command;
    }

    @Override
    protected ServiceContext getContext() {
        return context;
    }

    @Override
    protected String getExpectedLog() {
        return expectedLog;
    }

    @Override
    public void init(Annotation annotation) {
        KeycloakContainer metadata = (KeycloakContainer) annotation;
        this.image = PropertiesUtils.resolveProperty(metadata.image());
        this.restPort = metadata.port();
        this.expectedLog = PropertiesUtils.resolveProperty(metadata.expectedLog());
        this.command = metadata.command();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        for (KeycloakContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        return new KeycloakGenericDockerContainerManagedResource(this);
    }
}
