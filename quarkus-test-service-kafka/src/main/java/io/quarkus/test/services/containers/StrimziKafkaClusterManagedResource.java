package io.quarkus.test.services.containers;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;
import io.quarkus.test.utils.DockerUtils;
import io.strimzi.test.container.StrimziKafkaCluster;

/**
 * Kafka resource backed by the upstream {@code StrimziKafkaCluster}.
 */
public class StrimziKafkaClusterManagedResource extends BaseKafkaContainerManagedResource {

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
        StrimziKafkaCluster cluster = new StrimziKafkaCluster.StrimziKafkaClusterBuilder()
                .withNumberOfBrokers(1)
                .withImage(getKafkaImage())
                .withContainerCustomizer(broker -> broker
                        .withNetwork(getNetwork())
                        .withCreateContainerCmdModifier(cmd -> cmd.withName(DockerUtils.generateDockerContainerName())))
                .build();

        return cluster.getBrokers().iterator().next();
    }

    @Override
    protected GenericContainer<?> initRegistryContainer(GenericContainer<?> kafka) {
        throw new UnsupportedOperationException(
                "Schema registry is not supported yet by " + StrimziKafkaClusterManagedResource.class.getName());
    }

    private String getKafkaImage() {
        return getKafkaImageName() + ":" + getKafkaVersion();
    }

    /**
     * Everything rejected here still has to use {@link KafkaVendor#STRIMZI} until it is migrated.
     */
    @Override
    public void validate() {
        if (model.getProtocol() != KafkaProtocol.PLAIN_TEXT) {
            // SSL and SASL are driven by a server.properties file today, and SCRAM has no equivalent upstream,
            // as StrimziKafkaContainer#configureAuthentication rejects it
            Assertions.fail("Only PLAIN_TEXT protocol is supported so far by " + KafkaVendor.STRIMZI_CLUSTER);
        }

        if (StringUtils.isNotEmpty(model.getServerProperties())) {
            // StrimziKafkaContainer#containerIsStarting always overwrites /opt/kafka/config/kraft/server.properties,
            // so the file copied into the container would be silently dropped. Kafka configuration has to be passed
            // through withKafkaConfigurationMap instead.
            Assertions.fail("Custom server.properties is not supported by " + KafkaVendor.STRIMZI_CLUSTER
                    + ", as the upstream container overwrites them");
        }

        if (model.getKafkaConfigResources().length > 0) {
            Assertions.fail("Custom kafka config resources are not supported yet by " + KafkaVendor.STRIMZI_CLUSTER);
        }

        if (model.isWithRegistry()) {
            // the registry would replace the cluster network with its own, breaking the broker-0 alias that the
            // generated controller.quorum.voters relies on
            Assertions.fail("Schema registry is not supported yet by " + KafkaVendor.STRIMZI_CLUSTER);
        }
    }
}
