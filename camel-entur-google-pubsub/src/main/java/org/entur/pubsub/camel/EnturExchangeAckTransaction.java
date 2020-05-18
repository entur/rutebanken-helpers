package org.entur.pubsub.camel;

import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.entur.pubsub.base.EnturGooglePubSubException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;

public class EnturExchangeAckTransaction implements Synchronization {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Ack successful exchanges.
     *
     * @param exchange
     */
    @Override
    public void onComplete(Exchange exchange) {
        logger.debug("Acknowledging message after successful processing for Exchange {}", exchange.getExchangeId());
        getAck(exchange).ack();
    }

    /**
     * Negatively ack failed exchanges.
     *
     * @param exchange
     */
    @Override
    public void onFailure(Exchange exchange) {
        logger.debug("Acknowledging message after failed processing for Exchange {}", exchange.getExchangeId());
        getAck(exchange).nack();
    }

    private BasicAcknowledgeablePubsubMessage getAck(Exchange exchange) {
        BasicAcknowledgeablePubsubMessage ack = (BasicAcknowledgeablePubsubMessage) exchange.getIn().getHeader(EnturGooglePubSubConstants.ACK_ID);
        if(ack == null) {
            String routeId = exchange.getFromRouteId();
            throw new EnturGooglePubSubException("The exchange does not contain any ack header. Check that the Camel route " + routeId + " does not mistakenly delete it with a call to removeHeaders()");
        }
        return ack;
    }
}


