package io.quarkus.qe.kafka.producers;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import org.jboss.logging.Logger;

import io.quarkus.qe.kafka.StockPrice;
import io.quarkus.qe.kafka.config.VertxKProducerConfig;
import io.quarkus.qe.kafka.status;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class StockPriceProducer {

    private static final Logger LOG = Logger.getLogger(StockPriceProducer.class);

    @Inject
    VertxKProducerConfig config;

    @Inject
    @Channel("source-stock-price")
    @OnOverflow(value = OnOverflow.Strategy.DROP)
    Emitter<StockPrice> emitter;

    private Random random = new Random();

    public Uni<Void> generate() {
        IntStream.range(0, config.batchSize()).forEach(next -> {
            StockPrice event = StockPrice.newBuilder().setId("IBM").setPrice(random.nextDouble()).setStatus(status.PENDING)
                    .build();
            LOG.debugv("PRODUCER -> ID: {0}, PRICE: {1}", event.getId(), event.getPrice());
            emitter.send(event).whenComplete(handlerEmitterResponse(StockPriceProducer.class.getName()));
        });

        return Uni.createFrom().voidItem();
    }

    private BiConsumer<Void, Throwable> handlerEmitterResponse(String owner) {
        return (success, failure) -> {
            if (failure != null) {
                LOG.warn(String.format("D'oh! %s", failure.getMessage()));
            } else {
                LOG.debug(String.format("Message sent successfully. Owner %s", owner));
            }
        };
    }
}
