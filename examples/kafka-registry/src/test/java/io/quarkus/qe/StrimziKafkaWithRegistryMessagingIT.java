
package io.quarkus.qe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.KafkaService;
import io.quarkus.test.bootstrap.Protocol;
import io.quarkus.test.bootstrap.RestService;
import io.quarkus.test.scenarios.QuarkusScenario;
import io.quarkus.test.services.KafkaContainer;
import io.quarkus.test.services.QuarkusApplication;
import io.quarkus.test.services.containers.model.KafkaVendor;

@QuarkusScenario
public class StrimziKafkaWithRegistryMessagingIT {

    @KafkaContainer(vendor = KafkaVendor.STRIMZI, withRegistry = true, registryPath = "/apis/registry/v2")
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperties("strimzi-application.properties")
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl)
            .withProperty("strimzi.registry.url", kafka::getRegistryUrl);

    @Test
    public void producerConsumesTest() throws InterruptedException {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(app.getURI(Protocol.HTTP) + "/stock/stream");

        final CountDownLatch latch = new CountDownLatch(1);

        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> latch.countDown());
        source.open();
        boolean completed = latch.await(5, TimeUnit.MINUTES);
        assertTrue(completed);
        source.close();
    }
}
