package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.utils.DockerUtils;

public class ConfluentKafkaContainerManagedResource extends BaseKafkaContainerManagedResource {

    protected ConfluentKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    public String getDisplayName() {
        return getKafkaImage() + ":" + getKafkaVersion();
    }

    @Override
    protected GenericContainer<?> initKafkaContainer() {
        return new KafkaContainer(DockerImageName.parse(getKafkaImage() + ":" + getKafkaVersion()))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        GenericContainer<?> schemaRegistry = new GenericContainer<>(DockerImageName.parse(getKafkaRegistryImage()));
        schemaRegistry.withExposedPorts(getKafkaRegistryPort());
        schemaRegistry.withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
        schemaRegistry.withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + getKafkaRegistryPort());
        schemaRegistry.withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                "PLAINTEXT://" + kafka.getNetworkAliases().get(0) + ":9092");
        schemaRegistry.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        return schemaRegistry;
    }

    protected String getKafkaImage() {
        return StringUtils.defaultIfBlank(model.getImage(), model.getVendor().getImage());
    }

}
