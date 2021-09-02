package io.quarkus.qe.tcp;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.AmqService;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.AmqContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.AmqProtocol;

@QuarkusScenario
public class AmqpAmqIT {

    static final List<String> EXPECTED_PRICES = Arrays.asList("10", "20", "30", "40", "50", "60", "70", "80", "90", "100");

    @AmqContainer(protocol = AmqProtocol.AMQP)
    static final AmqService amq = new AmqService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("amqp-host", amq::getAmqpHost)
            .withProperty("amqp-port", () -> "" + amq.getPort());

    /**
     * There is a PriceProducer that pushes a new integer "price" to a JMS queue
     * called "prices" each second. PriceConsumer is a loop that starts at the
     * beginning of the application runtime and blocks on reading from the queue
     * called "prices". Once a value is read, the attribute lastPrice is updated.
     *
     * This test merely checks that the value was updated. It is the most basic
     * sanity check that JMS is up and running.
     */
    @Test
    public void testLastPrice() {
        await().pollInterval(1, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
                    String response = app.given().get("/price")
                            .then().statusCode(HttpStatus.SC_OK).extract().asString();
                    assertTrue(EXPECTED_PRICES.stream().anyMatch(response::contains),
                            "Expected prices not found in " + response);
                });
    }
}
