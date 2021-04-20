package io.quarkus.qe.amqp;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PriceConsumer {

    private static final Logger LOG = Logger.getLogger(PriceProducer.class.getName());

    private final ConcurrentLinkedQueue<Integer> prices = new ConcurrentLinkedQueue<>();

    @Incoming("prices")
    public void process(Integer price) {
        LOG.info("process fired: " + price);
        this.prices.add(price);
    }

    public Queue<Integer> getPrices() {
        return prices;
    }
}
