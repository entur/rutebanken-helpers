package org.entur.pubsub.camel;

import org.apache.camel.Exchange;
import org.apache.camel.spi.Synchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;

import java.util.ArrayList;
import java.util.List;

public class EnturExchangeAckTransaction implements Synchronization {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public void onComplete(Exchange exchange) {
        logger.debug("Acknowledging message after successful processing for Exchange " + exchange.getExchangeId());
        ack(getAckList(exchange));
    }


    /**
     * Negatively ack failed messages.
     *
     * @param exchange
     */
    @Override
    public void onFailure(Exchange exchange) {
        logger.debug("Acknowledging message after failed processing for Exchange " + exchange.getExchangeId());
        nack(getAckList(exchange));
    }


    private void ack(List<BasicAcknowledgeablePubsubMessage> ackList) {
        for (BasicAcknowledgeablePubsubMessage ack : ackList) {
            ack.ack();
        }
    }

    private void nack(List<BasicAcknowledgeablePubsubMessage> ackList) {
        for (BasicAcknowledgeablePubsubMessage ack : ackList) {
            ack.nack();
        }
    }

    private List<BasicAcknowledgeablePubsubMessage> getAckList(Exchange exchange) {
        List<BasicAcknowledgeablePubsubMessage> ackList = new ArrayList<>();

        if (null != exchange.getProperty(Exchange.GROUPED_EXCHANGE)) {
            for (Exchange ex : (List<Exchange>) exchange.getProperty(Exchange.GROUPED_EXCHANGE, List.class)) {
                BasicAcknowledgeablePubsubMessage ack = (BasicAcknowledgeablePubsubMessage) ex.getIn().getHeader(EnturGooglePubSubConstants.ACK_ID);
                if (null != ack) {
                    ackList.add(ack);
                }
            }
        } else {
            ackList.add((BasicAcknowledgeablePubsubMessage) exchange.getIn().getHeader(EnturGooglePubSubConstants.ACK_ID));
        }

        return ackList;
    }
}


