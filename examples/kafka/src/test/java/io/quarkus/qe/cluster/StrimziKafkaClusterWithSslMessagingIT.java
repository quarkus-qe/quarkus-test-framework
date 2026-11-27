package io.quarkus.qe.cluster;

import java.time.Duration;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.KafkaProtocol;
import io.quarkus.test.services.containers.model.KafkaVendor;

/**
 * One way TLS on a broker started through the upstream {@code StrimziKafkaCluster}.
 * <p>
 * Unlike {@link io.quarkus.qe.StrimziKafkaWithDefaultSslMessagingIT}, the stores are not checked in. They are generated per run
 * and
 * the client side of them is handed over by {@code KafkaService#getSslProperties}.
 */
@QuarkusScenario
public class StrimziKafkaClusterWithSslMessagingIT {

    @KafkaContainer(vendor = KafkaVendor.STRIMZI_CLUSTER, protocol = KafkaProtocol.SSL)
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl)
            .withProperties(kafka::getSslProperties);

    @Test
    public void checkUserResourceByNormalUser() {
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            app.given().get("/prices/poll")
                    .then()
                    .statusCode(HttpStatus.SC_OK);
        });
    }
}
