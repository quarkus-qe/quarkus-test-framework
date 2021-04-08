package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.containers.model.KafkaVendor;

public class KafkaContainerManagedResourceBuilder implements ManagedResourceBuilder {

    private final ServiceLoader<KafkaContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(KafkaContainerManagedResourceBinding.class);

    private ServiceContext context;
    private KafkaVendor vendor;
    private String version;
    private boolean withRegistry;

    protected KafkaVendor getVendor() {
        return vendor;
    }

    protected String getVersion() {
        return version;
    }

    protected ServiceContext getContext() {
        return context;
    }

    protected boolean isWithRegistry() {
        return withRegistry;
    }

    @Override
    public void init(Annotation annotation) {
        KafkaContainer metadata = (KafkaContainer) annotation;
        this.vendor = metadata.vendor();
        this.version = metadata.version();
        this.withRegistry = metadata.withRegistry();
    }

    @Override
    public ManagedResource build(ServiceContext context) {
        this.context = context;
        for (KafkaContainerManagedResourceBinding binding : managedResourceBindingsRegistry) {
            if (binding.appliesFor(this)) {
                return binding.init(this);
            }
        }

        if (vendor == KafkaVendor.STRIMZI) {
            return new StrimziKafkaContainerManagedResource(this);
        }

        return new ConfluentKafkaContainerManagedResource(this);
    }
}
