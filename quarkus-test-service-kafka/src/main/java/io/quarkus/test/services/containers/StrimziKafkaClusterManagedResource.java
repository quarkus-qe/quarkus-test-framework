package io.quarkus.test.services.containers;

import static io.quarkus.test.services.Certificate.Format.PKCS12;
import static java.util.Map.entry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.security.certificate.Certificate;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;
import io.quarkus.test.utils.DockerUtils;
import io.strimzi.test.container.StrimziKafkaCluster;
import io.strimzi.test.container.StrimziKafkaContainer;

/**
 * Kafka resource backed by the upstream {@code StrimziKafkaCluster}.
 */
public class StrimziKafkaClusterManagedResource extends BaseKafkaContainerManagedResource {

    private static final String CERTIFICATE_PREFIX = "strimzi-server-ssl";
    private static final String CERTIFICATE_PASSWORD = "top-secret";
    private static final String PKCS12_STORE_TYPE = "PKCS12";
    private static final String SASL_USERNAME = "client";
    private static final String SASL_PASSWORD = "client-secret";
    private static final String SCRAM_MECHANISM = "SCRAM-SHA-512";
    private static final String SCRAM_PASSWORD = "client-secret12345678912345678912";

    private StrimziKafkaContainer kafkaContainer;

    protected StrimziKafkaClusterManagedResource(KafkaContainerManagedResourceBuilder model) {
        super(model);
    }

    @Override
    public String getDisplayName() {
        return getKafkaImage();
    }

    @Override
    protected GenericContainer<?> initKafkaContainer() {
        // the cluster is discarded once the broker is taken out of it, so the broker joins a network that is closed
        // with this resource instead of the one created by the cluster
        Consumer<StrimziKafkaContainer> customizer = broker -> broker
                .withNetwork(getNetwork())
                .withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));

        var builder = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                .withNumberOfBrokers(1)
                .withImage(getKafkaImage());

        if (model.getProtocol() == KafkaProtocol.SSL || model.getProtocol() == KafkaProtocol.SASL_SSL) {
            String securityProtocol = model.getProtocol() == KafkaProtocol.SSL ? "SSL" : "SASL_SSL";
            Certificate certificate = Certificate.of(CERTIFICATE_PREFIX, PKCS12, CERTIFICATE_PASSWORD);
            String keyStore = model.getKafkaConfigPath() + Path.of(certificate.keystorePath()).getFileName();
            String trustStore = model.getKafkaConfigPath() + Path.of(certificate.truststorePath()).getFileName();

            customizer = customizer.andThen(broker -> broker
                    .withCopyFileToContainer(MountableFile.forHostPath(certificate.keystorePath()), keyStore)
                    .withCopyFileToContainer(MountableFile.forHostPath(certificate.truststorePath()), trustStore));

            builder.withBootstrapServers(
                    broker -> securityProtocol + "://" + broker.getHost() + ":" + broker.getMappedPort(getTargetPort()))
                    .withAdditionalKafkaConfiguration(Map.ofEntries(
                            entry("listener.security.protocol.map",
                                    securityProtocol + ":" + securityProtocol + ",BROKER1:PLAINTEXT,CONTROLLER:PLAINTEXT"),
                            entry("ssl.keystore.location", keyStore),
                            entry("ssl.keystore.password", CERTIFICATE_PASSWORD),
                            entry("ssl.keystore.type", PKCS12_STORE_TYPE),
                            entry("ssl.key.password", CERTIFICATE_PASSWORD),
                            entry("ssl.truststore.location", trustStore),
                            entry("ssl.truststore.password", CERTIFICATE_PASSWORD),
                            entry("ssl.truststore.type", PKCS12_STORE_TYPE),
                            entry("ssl.endpoint.identification.algorithm", "")));

            Map<String, String> clientProperties = new HashMap<>();
            clientProperties.put("kafka.ssl.enable", "true");
            clientProperties.put("kafka.security.protocol", securityProtocol);
            String tlsConfigName = model.getQuarkusTlsRegistryConfigName();
            if (tlsConfigName == null) {
                clientProperties.put("kafka.ssl.truststore.location", certificate.truststorePath());
                clientProperties.put("kafka.ssl.truststore.password", CERTIFICATE_PASSWORD);
                clientProperties.put("kafka.ssl.truststore.type", PKCS12_STORE_TYPE);
            } else {
                clientProperties.put("kafka.tls-configuration-name", tlsConfigName);
                clientProperties.put("quarkus.tls." + tlsConfigName + ".trust-store.p12.path", certificate.truststorePath());
                clientProperties.put("quarkus.tls." + tlsConfigName + ".trust-store.p12.password", CERTIFICATE_PASSWORD);
            }

            if (model.getProtocol() == KafkaProtocol.SASL_SSL) {
                // the user itself is added once the broker is up, see #addScramUser
                builder.withAdditionalKafkaConfiguration(Map.of(
                        "sasl.enabled.mechanisms", SCRAM_MECHANISM,
                        "listener.name.sasl_ssl.scram-sha-512.sasl.jaas.config",
                        "org.apache.kafka.common.security.scram.ScramLoginModule required;"));
                clientProperties.put("kafka.sasl.mechanism", SCRAM_MECHANISM);
                clientProperties.put("kafka.sasl.jaas.config",
                        "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + SASL_USERNAME
                                + "\" password=\"" + SCRAM_PASSWORD + "\";");
            }
            model.getContext().put(KafkaService.KAFKA_SSL_PROPERTIES, Map.copyOf(clientProperties));
        } else if (model.getProtocol() == KafkaProtocol.SASL) {
            // the inter-broker listener stays on plain text, so only the client user is needed
            builder.withBootstrapServers(
                    broker -> "SASL_PLAINTEXT://" + broker.getHost() + ":" + broker.getMappedPort(getTargetPort()))
                    .withAdditionalKafkaConfiguration(Map.of(
                            "listener.security.protocol.map",
                            "SASL_PLAINTEXT:SASL_PLAINTEXT,BROKER1:PLAINTEXT,CONTROLLER:PLAINTEXT",
                            "sasl.enabled.mechanisms", "PLAIN",
                            "listener.name.sasl_plaintext.plain.sasl.jaas.config",
                            "org.apache.kafka.common.security.plain.PlainLoginModule required user_" + SASL_USERNAME
                                    + "=\"" + SASL_PASSWORD + "\";"));
        }

        kafkaContainer = builder.withContainerCustomizer(customizer).build().getBrokers().iterator().next();
        return kafkaContainer;
    }

    @Override
    public void start() {
        super.start();

        if (model.getProtocol() == KafkaProtocol.SASL_SSL) {
            // newer upstream versions prefix it with the PLAINTEXT scheme
            String bootstrapServers = kafkaContainer.getNetworkBootstrapServers();
            if (bootstrapServers.startsWith("PLAINTEXT://")) {
                bootstrapServers = bootstrapServers.substring("PLAINTEXT://".length());
            }
            try {
                ExecResult result = kafkaContainer.execInContainer("bin/kafka-configs.sh",
                        "--bootstrap-server", bootstrapServers,
                        "--alter", "--entity-type", "users", "--entity-name", SASL_USERNAME,
                        "--add-config", SCRAM_MECHANISM + "=[password=" + SCRAM_PASSWORD + "]");
                if (result.getExitCode() != 0) {
                    throw new IllegalStateException("Failed to add SCRAM user " + SASL_USERNAME + ": " + result.getStderr());
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        return new GenericContainer<>(getKafkaRegistryImage())
                .withExposedPorts(getKafkaRegistryPort())
                .withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName()));
    }

    private String getKafkaImage() {
        return getKafkaImageName() + ":" + getKafkaVersion();
    }

    /**
     * Everything rejected here still has to use {@link KafkaVendor#STRIMZI} until it is migrated.
     */
    @Override
    public void validate() {
        if (StringUtils.isNotEmpty(model.getServerProperties())) {
            // StrimziKafkaContainer#containerIsStarting always overwrites /opt/kafka/config/kraft/server.properties,
            // so the file copied into the container would be silently dropped. Kafka configuration has to be passed
            // through withKafkaConfigurationMap instead.
            Assertions.fail("Custom server.properties is not supported by " + KafkaVendor.STRIMZI_CLUSTER
                    + ", as the upstream container overwrites them");
        }

        if (model.getKafkaConfigResources().length > 0) {
            // for SSL this also rules out tests bringing their own trust store, they get a generated one instead
            Assertions.fail("Custom kafka config resources are not supported yet by " + KafkaVendor.STRIMZI_CLUSTER);
        }
    }
}
