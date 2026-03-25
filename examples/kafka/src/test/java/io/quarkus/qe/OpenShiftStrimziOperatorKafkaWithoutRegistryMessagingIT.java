package io.quarkus.qe;

import java.time.Duration;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.OpenShiftScenario;
import io.quarkus.test.services.Operator;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.operator.KafkaInstance;

@OpenShiftScenario
public class OpenShiftStrimziOperatorKafkaWithoutRegistryMessagingIT {

    // TODO: Update when OCP 4.14 is unsupported; this is the last version that works on OCP 4.14
    @Operator(name = "strimzi-kafka-operator", channel = "strimzi-0.50.x")
    static final KafkaInstance kafka = new KafkaInstance();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka.getBootstrapUrl());

    @Test
    public void checkUserResourceByNormalUser() {
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            app.given().get("/prices/poll")
                    .then()
                    .statusCode(HttpStatus.SC_OK);
        });
    }
}
