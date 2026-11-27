package io.quarkus.qe.cluster;

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

/**
 * Same scenario as {@link io.quarkus.qe.AlertMonitorIT}, but started through the upstream {@code StrimziKafkaCluster}.
 */
@QuarkusScenario
public class StrimziKafkaClusterAlertMonitorIT {

    @KafkaContainer(vendor = KafkaVendor.STRIMZI_CLUSTER)
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl)
            .withProperty("quarkus.kafka-streams.bootstrap-servers", kafka::getBootstrapUrl)
            .withProperty("quarkus.kafka-streams.application-id", "login-denied-aggregator-cluster");

    @Test
    void testAlertMonitorEventStream() throws InterruptedException {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(app.getURI(Protocol.HTTP) + "/monitor/stream");

        final CountDownLatch latch = new CountDownLatch(1);

        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> latch.countDown());
        source.open();
        boolean completed = latch.await(5, TimeUnit.MINUTES);
        assertTrue(completed);
        source.close();
    }
}
