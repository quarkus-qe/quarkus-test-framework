package io.quarkus.test.services.containers;

import static io.quarkus.test.services.Certificate.Format.PKCS12;
import static java.util.Map.entry;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
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

        if (model.getProtocol() == KafkaProtocol.SSL) {
            Certificate certificate = Certificate.of(CERTIFICATE_PREFIX, PKCS12, CERTIFICATE_PASSWORD);
            String keyStore = model.getKafkaConfigPath() + Path.of(certificate.keystorePath()).getFileName();
            String trustStore = model.getKafkaConfigPath() + Path.of(certificate.truststorePath()).getFileName();

            customizer = customizer.andThen(broker -> broker
                    .withCopyFileToContainer(MountableFile.forHostPath(certificate.keystorePath()), keyStore)
                    .withCopyFileToContainer(MountableFile.forHostPath(certificate.truststorePath()), trustStore));

            builder.withBootstrapServers(
                    broker -> "SSL://" + broker.getHost() + ":" + broker.getMappedPort(getTargetPort()))
                    .withAdditionalKafkaConfiguration(Map.ofEntries(
                            entry("listener.security.protocol.map", "SSL:SSL,BROKER1:PLAINTEXT,CONTROLLER:PLAINTEXT"),
                            entry("ssl.keystore.location", keyStore),
                            entry("ssl.keystore.password", CERTIFICATE_PASSWORD),
                            entry("ssl.keystore.type", PKCS12_STORE_TYPE),
                            entry("ssl.key.password", CERTIFICATE_PASSWORD),
                            entry("ssl.truststore.location", trustStore),
                            entry("ssl.truststore.password", CERTIFICATE_PASSWORD),
                            entry("ssl.truststore.type", PKCS12_STORE_TYPE),
                            entry("ssl.endpoint.identification.algorithm", "")));
            /*
             * What the application under test needs to talk to the broker, read back through
             * KafkaService#getSslProperties. Either as plain Kafka client properties, or through the Quarkus TLS
             * registry when the test asked for a named configuration.
             */
            String tlsConfigName = model.getQuarkusTlsRegistryConfigName();
            model.getContext().put(KafkaService.KAFKA_SSL_PROPERTIES, tlsConfigName == null
                    ? Map.of("kafka.ssl.enable", "true",
                            "kafka.security.protocol", "SSL",
                            "kafka.ssl.truststore.location", certificate.truststorePath(),
                            "kafka.ssl.truststore.password", CERTIFICATE_PASSWORD,
                            "kafka.ssl.truststore.type", PKCS12_STORE_TYPE)
                    : Map.of("kafka.ssl.enable", "true",
                            "kafka.security.protocol", "SSL",
                            "kafka.tls-configuration-name", tlsConfigName,
                            "quarkus.tls." + tlsConfigName + ".trust-store.p12.path", certificate.truststorePath(),
                            "quarkus.tls." + tlsConfigName + ".trust-store.p12.password", CERTIFICATE_PASSWORD));
        }

        return builder.withContainerCustomizer(customizer).build().getBrokers().iterator().next();
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
        if (model.getProtocol() != KafkaProtocol.PLAIN_TEXT && model.getProtocol() != KafkaProtocol.SSL) {
            // SCRAM has no equivalent upstream, StrimziKafkaContainer#configureAuthentication rejects it and the
            // format command never gets --add-scram
            Assertions.fail("Only PLAIN_TEXT and SSL protocols are supported so far by " + KafkaVendor.STRIMZI_CLUSTER);
        }

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
