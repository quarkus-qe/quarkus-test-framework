package io.quarkus.test.services.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class ConfluentKafkaContainerManagedResource extends BaseKafkaContainerManagedResource {

    private static final String REGISTRY_IMAGE = "confluentinc/cp-schema-registry:";
    private static final int REGISTRY_PORT = 8081;

    protected ConfluentKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected int getTargetPort() {
        return KafkaContainer.KAFKA_PORT;
    }

    @Override
    protected int getRegistryTargetPort() {
        return REGISTRY_PORT;
    }

    @Override
    protected GenericContainer<?> initKafkaContainer() {
        return new KafkaContainer(DockerImageName.parse(model.getVendor().getImage() + ":" + getKafkaVersion()));
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        GenericContainer<?> schemaRegistry = new GenericContainer<>(REGISTRY_IMAGE + getKafkaVersion());
        schemaRegistry.withExposedPorts(REGISTRY_PORT);
        schemaRegistry.withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
        schemaRegistry.withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + REGISTRY_PORT);
        schemaRegistry.withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                "PLAINTEXT://" + kafka.getNetworkAliases().get(0) + ":9092");
        return schemaRegistry;
    }

}
