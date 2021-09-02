package io.quarkus.test.services.containers;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.services.containers.model.KafkaVendor;
import io.strimzi.StrimziKafkaContainer;

public class StrimziKafkaContainerManagedResource extends BaseKafkaContainerManagedResource {

    protected StrimziKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    public String getDisplayName() {
        return KafkaVendor.STRIMZI.getImage() + getKafkaVersion();
    }

    @Override
    protected GenericContainer<?> initKafkaContainer() {
        return new StrimziKafkaContainer(getKafkaVersion());
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        GenericContainer<?> schemaRegistry = new GenericContainer<>(getKafkaRegistryImage());
        schemaRegistry.withExposedPorts(getKafkaRegistryPort());
        schemaRegistry.withEnv("APPLICATION_ID", "registry_id");
        schemaRegistry.withEnv("APPLICATION_SERVER", "localhost:9000");
        schemaRegistry.withEnv("KAFKA_BOOTSTRAP_SERVERS", "PLAINTEXT://localhost:" + getTargetPort());
        return schemaRegistry;
    }
}
