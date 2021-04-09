package io.quarkus.test.services.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;

import io.strimzi.StrimziKafkaContainer;

public class StrimziKafkaContainerManagedResource extends BaseKafkaContainerManagedResource {

    private static final String REGISTRY_IMAGE = "apicurio/apicurio-registry-mem:1.2.2.Final";
    private static final int REGISTRY_PORT = 8080;
    private static final String REGISTRY_PATH = "/api";
    private static final int KAFKA_PORT = 9092;

    protected StrimziKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    protected int getTargetPort() {
        return KAFKA_PORT;
    }

    @Override
    protected int getRegistryTargetPort() {
        return REGISTRY_PORT;
    }

    @Override
    protected GenericContainer<?> initKafkaContainer() {
        return new StrimziKafkaContainer(getKafkaVersion());
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        GenericContainer<?> schemaRegistry = new GenericContainer<>(REGISTRY_IMAGE);
        schemaRegistry.withExposedPorts(REGISTRY_PORT);
        schemaRegistry.withEnv("APPLICATION_ID", "registry_id");
        schemaRegistry.withEnv("APPLICATION_SERVER", "localhost:9000");
        schemaRegistry.withEnv("KAFKA_BOOTSTRAP_SERVERS", "PLAINTEXT://localhost:" + KafkaContainer.KAFKA_PORT);
        return schemaRegistry;
    }

    @Override
    protected String getRegistryPath() {
        return REGISTRY_PATH;
    }
}
