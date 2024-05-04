package io.quarkus.test.services.containers;

import java.lang.annotation.Annotation;
import java.util.ServiceLoader;

import io.quarkus.test.bootstrap.LocalhostManagedResource;
import io.quarkus.test.bootstrap.ManagedResource;
import io.quarkus.test.bootstrap.ManagedResourceBuilder;
import io.quarkus.test.bootstrap.ServiceContext;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;
import io.quarkus.test.utils.PropertiesUtils;

public class KafkaContainerManagedResourceBuilder implements ManagedResourceBuilder {
    private final ServiceLoader<KafkaContainerManagedResourceBinding> managedResourceBindingsRegistry = ServiceLoader
            .load(KafkaContainerManagedResourceBinding.class);

    private ServiceContext context;
    private KafkaVendor vendor;
    private String image;
    private String version;
    private boolean withRegistry;
    private String registryImage;
    private String registryPath;
    private KafkaProtocol protocol = KafkaProtocol.PLAIN_TEXT;
    private String kafkaConfigPath;
    private String serverProperties;
    private String[] kafkaConfigResources;

    protected KafkaVendor getVendor() {
        return vendor;
    }

    protected String getImage() {
        return image;
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

    protected String getDefaultRegistryImageVersion() {
        String defaultImage = getVendor().getRegistry().getImage();
        String defaultVersion = getVendor().getRegistry().getDefaultVersion();
        return defaultImage + ":" + defaultVersion;
    }

    protected String getRegistryImage() {
        return registryImage;
    }

    protected String getRegistryPath() {
        return registryPath;
    }

    protected KafkaProtocol getProtocol() {
        return protocol;
    }

    protected String getKafkaConfigPath() {
        return kafkaConfigPath;
    }

    protected String getServerProperties() {
        return serverProperties;
    }

    protected String[] getKafkaConfigResources() {
        return kafkaConfigResources;
    }

    protected String getRegistryImageVersion() {
        String registryImage = getDefaultRegistryImageVersion();
        if (!getRegistryImage().isEmpty()) {
            String defaultVersion = getVendor().getRegistry().getDefaultVersion();
            registryImage = getRegistryImage();
            if (!registryImage.contains(":")) {
                registryImage += ":" + defaultVersion;
            }
        }

        return registryImage;
    }

    @Override
    public void init(Annotation annotation) {
        KafkaContainer metadata = (KafkaContainer) annotation;
        this.vendor = metadata.vendor();
        this.image = PropertiesUtils.resolveProperty(metadata.image());
        this.version = PropertiesUtils.resolveProperty(metadata.version());
        this.withRegistry = metadata.withRegistry();
        this.registryImage = PropertiesUtils.resolveProperty(metadata.registryImage());
        this.registryPath = PropertiesUtils.resolveProperty(metadata.registryPath());
        this.protocol = metadata.protocol();
        this.kafkaConfigPath = PropertiesUtils.resolveProperty(metadata.kafkaConfigPath());
        this.serverProperties = PropertiesUtils.resolveProperty(metadata.serverProperties());
        this.kafkaConfigResources = metadata.kafkaConfigResources();
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
            return new LocalhostManagedResource(new StrimziKafkaContainerManagedResource(this));
        }

        return new ConfluentKafkaContainerManagedResource(this);
    }
}
