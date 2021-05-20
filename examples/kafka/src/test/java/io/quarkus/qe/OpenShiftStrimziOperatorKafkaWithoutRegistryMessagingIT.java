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

    @Operator(name = "strimzi-kafka-operator")
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
