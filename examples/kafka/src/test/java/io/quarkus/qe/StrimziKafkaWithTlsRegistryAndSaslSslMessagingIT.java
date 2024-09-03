package io.quarkus.qe;

import static org.hamcrest.Matchers.is;

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

@QuarkusScenario
public class StrimziKafkaWithTlsRegistryAndSaslSslMessagingIT {

    private static final String TLS_CONFIG_NAME = "tls-config-name-1";

    @KafkaContainer(vendor = KafkaVendor.STRIMZI, protocol = KafkaProtocol.SASL_SSL, tlsConfigName = TLS_CONFIG_NAME, tlsRegistryEnabled = true)
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperties(kafka::getSslProperties)
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl);

    @Test
    public void checkUserResourceByNormalUser() {
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            app.given().get("/prices/poll")
                    .then()
                    .statusCode(HttpStatus.SC_OK);
        });
    }

    @Test
    public void testTlsRegistryContainNamedTlsConfig() {
        // this is but a smoke test so that we know @KafkaContainer#tlsConfigName had effect
        // while the 'checkUserResourceByNormalUser' tests that configured truststore works
        app.given()
                .pathParam("tls-config-name", TLS_CONFIG_NAME)
                .get("/tls-registry/validate-config/{tls-config-name}")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(is("true"));
    }
}
