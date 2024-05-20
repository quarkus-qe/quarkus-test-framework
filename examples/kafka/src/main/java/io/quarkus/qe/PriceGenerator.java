package io.quarkus.qe;

import java.time.Duration;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import io.smallrye.mutiny.Multi;

/**
 * A bean producing random prices every 1 seconds.
 * The prices are written to a Kafka topic (prices). The Kafka configuration is specified in the application configuration.
 */
@ApplicationScoped
public class PriceGenerator {

    private static final int MAX_PRICE = 100;

    private final Random random = new Random();

    @Outgoing("generated-price")
    public Multi<Integer> generate() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onOverflow().drop()
                .map(tick -> random.nextInt(MAX_PRICE));
    }

}
