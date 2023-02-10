package io.quarkus.qe.amqp;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Multi;

@ApplicationScoped
public class PriceProducer {

    private static final int TEN = 10;
    private static final int HUNDRED = 100;
    private static final Logger LOG = Logger.getLogger(PriceProducer.class.getName());

    @Outgoing("generated-price")
    public Multi<Integer> generate() {
        LOG.info("generate fired...");
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onOverflow().drop()
                .map(tick -> ((tick.intValue() * TEN) % HUNDRED) + TEN);
    }
}
