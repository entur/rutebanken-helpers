package org.entur.pubsub.camel;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;
import org.apache.camel.Exchange;
import org.apache.camel.ExtendedExchange;
import org.apache.camel.Processor;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.support.DefaultConsumer;
import org.entur.pubsub.base.EnturGooglePubSubUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EnturGooglePubSubConsumer extends DefaultConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnturGooglePubSubConsumer.class);

    private final EnturGooglePubSubEndpoint endpoint;
    private final Processor processor;
    private final Synchronization ackStrategy;
    private final PubSubTemplate pubSubTemplate;

    private List<Subscriber> subscribers = new ArrayList<>();

    public EnturGooglePubSubConsumer(EnturGooglePubSubEndpoint endpoint, Processor processor, PubSubTemplate pubSubTemplate) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.processor = processor;
        this.ackStrategy = new EnturExchangeAckTransaction();
        this.pubSubTemplate = pubSubTemplate;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        LOGGER.info("Starting Google PubSub consumer for {}/{}", endpoint.getProjectId(), endpoint.getDestinationName());

        Consumer<BasicAcknowledgeablePubsubMessage> messageConsumer = new Consumer<BasicAcknowledgeablePubsubMessage>() {

            @Override
            public void accept(BasicAcknowledgeablePubsubMessage basicAcknowledgeablePubsubMessage) {

                PubsubMessage pubsubMessage = basicAcknowledgeablePubsubMessage.getPubsubMessage();

                byte[] body = pubsubMessage.getData().toByteArray();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Received message ID : {}", pubsubMessage.getMessageId());
                }

                Exchange exchange = endpoint.createExchange();
                exchange.getIn().setBody(body);

                exchange.getIn().setHeader(EnturGooglePubSubConstants.ACK_ID, basicAcknowledgeablePubsubMessage);
                exchange.getIn().setHeader(EnturGooglePubSubConstants.MESSAGE_ID, pubsubMessage.getMessageId());
                exchange.getIn().setHeader(EnturGooglePubSubConstants.PUBLISH_TIME, pubsubMessage.getPublishTime());

                Map<String, String> pubSubAttributes = pubsubMessage.getAttributesMap();
                if (pubSubAttributes != null) {
                    pubSubAttributes.entrySet()
                            .stream().filter(entry -> !entry.getKey().startsWith(EnturGooglePubSubConstants.GOOGLE_PUB_SUB_HEADER_PREFIX))
                            .forEach(entry -> exchange.getIn().setHeader(entry.getKey(), entry.getValue()));
                }

                if (endpoint.getAckMode() != EnturGooglePubSubConstants.AckMode.NONE) {
                    exchange.adapt(ExtendedExchange.class).addOnCompletion(EnturGooglePubSubConsumer.this.ackStrategy);
                }

                try {
                    processor.process(exchange);
                } catch (Exception e) {
                    exchange.setException(e);
                }
            }
        };

        for (int i = 0; i < endpoint.getConcurrentConsumers(); i++) {
            Subscriber subscriber = pubSubTemplate.subscribe(endpoint.getDestinationName(), messageConsumer);
            subscribers.add(subscriber);
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        LOGGER.info("Stopping Google PubSub consumer for subscription {}/{}", endpoint.getProjectId(), endpoint.getDestinationName());
        for (Subscriber subscriber : subscribers) {
            EnturGooglePubSubUtils.closeSubscriber(subscriber);
        }
        LOGGER.info("Stopped Google PubSub consumer for subscription {}/{}", endpoint.getProjectId(), endpoint.getDestinationName());
    }
}
