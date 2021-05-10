package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.AmqContainer;
import io.quarkus.test.services.containers.model.AmqProtocol;
import io.quarkus.test.utils.PropertiesUtils;

public class AmqContainerManagedResourceBuilder extends ContainerManagedResourceBuilder {

    private final ServiceLoader<AmqContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(AmqContainerManagedResourceBinding.class);

    private ServiceContext context;
    private String image;
    private String expectedLog;
    private AmqProtocol protocol;

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
        return protocol.getPort();
    }

    @Override
    protected ServiceContext getContext() {
        return context;
    }

    protected AmqProtocol getProtocol() {
        return protocol;
    }

    @Override
    public void init(Annotation annotation) {
        AmqContainer metadata = (AmqContainer) annotation;
        this.image = PropertiesUtils.resolveProperty(metadata.image());
        this.expectedLog = PropertiesUtils.resolveProperty(metadata.expectedLog());
        this.protocol = metadata.protocol();
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
