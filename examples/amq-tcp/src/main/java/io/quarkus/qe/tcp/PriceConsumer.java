package io.quarkus.qe.tcp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.Session;

/**
 * A bean consuming prices from the JMS queue.
 */
@ApplicationScoped
public class PriceConsumer {

    @Inject
    ConnectionFactory connectionFactory;

    public String getLastPrice() {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
                JMSConsumer consumer = context.createConsumer(context.createQueue("prices"))) {
            Message message = consumer.receive();
            if (message == null) {
                // receive returns `null` if the JMSConsumer is closed
                return "";
            }
            return message.getBody(String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
