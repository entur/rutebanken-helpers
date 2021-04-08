package org.entur.pubsub.base;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


/**
 * Base class for PubSub message consumers.
 * Consumers are started as late as possible after the Spring context initialization is complete,
 * and stopped as early as possible on context shutdown.
 */
@Component
public abstract class AbstractEnturGooglePubSubConsumer implements EnturGooglePubSubConsumer {

    @Autowired
    private EnturGooglePubSubAdmin enturGooglePubSubAdmin;

    @Autowired
    private PubSubTemplate pubSubTemplate;

    @Value("${entur.pubsub.consumer.retry.delay:15000}")
    private long retryDelay;

    private List<Subscriber> subscribers = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEnturGooglePubSubConsumer.class);

    protected abstract String getDestinationName();

    protected int getConcurrentConsumers() {
        return 1;
    }

    @EventListener
    public void handleContextRefreshed(ContextRefreshedEvent contextRefreshedEvent) {


        LOGGER.info("Initializing PubSub consumers for destination {}", getDestinationName());

        enturGooglePubSubAdmin.createSubscriptionIfMissing(getDestinationName());

        Consumer<BasicAcknowledgeablePubsubMessage> messageConsumer = new Consumer<BasicAcknowledgeablePubsubMessage>() {

            @Override
            public void accept(BasicAcknowledgeablePubsubMessage basicAcknowledgeablePubsubMessage) {
                PubsubMessage pubsubMessage = basicAcknowledgeablePubsubMessage.getPubsubMessage();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Received message ID : {}", pubsubMessage.getMessageId());
                }
                try {
                    onMessage(pubsubMessage.getData().toByteArray(), pubsubMessage.getAttributesMap());
                    basicAcknowledgeablePubsubMessage.ack();
                } catch (Exception e) {
                    basicAcknowledgeablePubsubMessage.nack();
                    LOGGER.error("Message processing failed, retrying in {} milliseconds", retryDelay, e);
                    delay(retryDelay);
                }
            }
        };
        for (int i = 0; i < getConcurrentConsumers(); i++) {
            Subscriber subscriber = pubSubTemplate.subscribe(getDestinationName(), messageConsumer);
            subscribers.add(subscriber);
        }

        LOGGER.info("Initialized PubSub consumers for destination {}", getDestinationName());

    }

    /**
     * Wait for the number of specified milliseconds.
     * @param delay delay in milliseconds
     */
    private static void delay(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EnturGooglePubSubException(e);
        }
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handleContextClosedEvent(ContextClosedEvent contextClosedEvent) {
        LOGGER.info("Stopping Google PubSub consumer for subscription {}", getDestinationName());
        for (Subscriber subscriber : subscribers) {
            EnturGooglePubSubUtils.closeSubscriber(subscriber);
        }
        LOGGER.info("Stopped Google PubSub consumer for subscription {}", getDestinationName());

    }

}
