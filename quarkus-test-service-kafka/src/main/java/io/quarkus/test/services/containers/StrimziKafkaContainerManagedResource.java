package io.quarkus.test.services.containers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.services.URILike;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;
import io.quarkus.test.services.containers.strimzi.ExtendedStrimziKafkaContainer;
import io.quarkus.test.utils.DockerUtils;

public class StrimziKafkaContainerManagedResource extends BaseKafkaContainerManagedResource {

    private static final String SSL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-ssl.properties";
    private static final String SSL_SERVER_KEYSTORE_DEFAULT = "strimzi-default-server-ssl-keystore.p12";
    private static final String SASL_SERVER_PROPERTIES_DEFAULT = "strimzi-default-server-sasl.properties";

    protected StrimziKafkaContainerManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    public String getDisplayName() {
        return KafkaVendor.STRIMZI.getImage() + getKafkaVersion();
    }

    @Override
    public URILike getURI(Protocol protocol) {
        var uri = super.getURI(protocol);
        if (model.getProtocol() == KafkaProtocol.SSL) {
            uri = uri.withScheme("SSL");
        } else if (model.getProtocol() == KafkaProtocol.SASL) {
            uri = uri.withScheme("SASL_PLAINTEXT");
        }
        return uri;
    }

    @Override
    protected GenericContainer<?> initKafkaContainer() {
        ExtendedStrimziKafkaContainer container = new ExtendedStrimziKafkaContainer(getKafkaImageName(), getKafkaVersion());
        if (StringUtils.isNotEmpty(getServerProperties())) {
            container.useCustomServerProperties();
        }
        container.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        return container;
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        GenericContainer<?> schemaRegistry = new GenericContainer<>(getKafkaRegistryImage());
        schemaRegistry.withExposedPorts(getKafkaRegistryPort());
        schemaRegistry.withEnv("APPLICATION_ID", "registry_id");
        schemaRegistry.withEnv("APPLICATION_SERVER", "localhost:9000");
        schemaRegistry.withEnv("KAFKA_BOOTSTRAP_SERVERS", "PLAINTEXT://localhost:" + getTargetPort());
        schemaRegistry.withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        return schemaRegistry;
    }

    @Override
    protected String getServerProperties() {
        // Return user server properties if set
        if (StringUtils.isNotEmpty(model.getServerProperties())) {
            return model.getServerProperties();
        }

        // If not, if ssl enabled, overwrite server properties
        if (model.getProtocol() == KafkaProtocol.SSL) {
            return SSL_SERVER_PROPERTIES_DEFAULT;
        } else if (model.getProtocol() == KafkaProtocol.SASL) {
            return SASL_SERVER_PROPERTIES_DEFAULT;
        }

        return super.getServerProperties();
    }

    @Override
    protected String[] getKafkaConfigResources() {
        List<String> effectiveUserKafkaConfigResources = new ArrayList<>();
        effectiveUserKafkaConfigResources.addAll(Arrays.asList(super.getKafkaConfigResources()));

        if (model.getProtocol() == KafkaProtocol.SSL) {
            effectiveUserKafkaConfigResources.add(SSL_SERVER_KEYSTORE_DEFAULT);
        }

        return effectiveUserKafkaConfigResources.toArray(new String[effectiveUserKafkaConfigResources.size()]);
    }
}
