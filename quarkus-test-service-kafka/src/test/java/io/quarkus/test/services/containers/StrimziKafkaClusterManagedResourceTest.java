package io.quarkus.test.services.containers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;

import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;

public class StrimziKafkaClusterManagedResourceTest {

    @KafkaContainer
    static Object plainText;

    @KafkaContainer(protocol = KafkaProtocol.SSL)
    static Object ssl;

    @KafkaContainer(withRegistry = true)
    static Object withRegistry;

    @KafkaContainer(protocol = KafkaProtocol.SASL)
    static Object sasl;

    @KafkaContainer(protocol = KafkaProtocol.SASL_SSL)
    static Object saslSsl;

    @KafkaContainer(serverProperties = "server.properties")
    static Object serverProperties;

    @KafkaContainer(kafkaConfigResources = "truststore.p12")
    static Object kafkaConfigResources;

    @ParameterizedTest
    @ValueSource(strings = { "plainText" })
    void supported(String field) {
        assertDoesNotThrow(() -> resourceFor(field).validate());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ssl", "withRegistry", "sasl", "saslSsl", "serverProperties", "kafkaConfigResources" })
    void unsupported(String field) {
        assertThrows(AssertionFailedError.class, () -> resourceFor(field).validate());
    }

    @Test
    void defaultVendor() throws NoSuchFieldException {
        assertEquals(KafkaVendor.STRIMZI_CLUSTER, builderFor("plainText").getVendor());
    }

    private static StrimziKafkaClusterManagedResource resourceFor(String field) throws NoSuchFieldException {
        return new StrimziKafkaClusterManagedResource(builderFor(field));
    }

    private static KafkaContainerManagedResourceBuilder builderFor(String field) throws NoSuchFieldException {
        KafkaContainerManagedResourceBuilder builder = new KafkaContainerManagedResourceBuilder();
        builder.init(StrimziKafkaClusterManagedResourceTest.class.getDeclaredField(field)
                .getAnnotation(KafkaContainer.class));
        return builder;
    }
}
