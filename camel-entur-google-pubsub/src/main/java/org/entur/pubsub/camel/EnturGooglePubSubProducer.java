package org.entur.pubsub.camel;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnturGooglePubSubProducer extends DefaultProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnturGooglePubSubProducer.class);

    private PubSubTemplate pubSubTemplate;

    public EnturGooglePubSubProducer(EnturGooglePubSubEndpoint endpoint, PubSubTemplate pubSubTemplate) {
        super(endpoint);
        this.pubSubTemplate = pubSubTemplate;
    }

    @Override
    public void process(Exchange exchange) {
        List<Exchange> entryList = prepareExchangeList(exchange);

        if (entryList == null || entryList.isEmpty()) {
            LOGGER.warn("The incoming message is either null or empty. Triggered by an aggregation timeout?");
            return;
        }

        sendMessages(entryList);
    }

    private static List<Exchange> prepareExchangeList(Exchange exchange) {

        List<Exchange> entryList;

        if (null == exchange.getProperty(Exchange.GROUPED_EXCHANGE)) {
            entryList = new ArrayList<>();
            entryList.add(exchange);
        } else {
            entryList = exchange.getProperty(Exchange.GROUPED_EXCHANGE, List.class);
        }

        return entryList;
    }

    private void sendMessages(List<Exchange> exchanges) {

        EnturGooglePubSubEndpoint endpoint = (EnturGooglePubSubEndpoint) getEndpoint();
        for (Exchange exchange : exchanges) {

            Object body = exchange.getIn().getBody();
            if (body == null) {
                body = "";
            }

            Map<String, String> pubSubAttributes = new HashMap<>();

            exchange.getIn().getHeaders().entrySet().stream()
                    // filter headers with null value (same behaviour as Camel ActiveMQ component)
                    .filter(entry -> entry.getValue() != null)
                    .filter(entry -> !entry.getKey().startsWith(EnturGooglePubSubConstants.GOOGLE_PUB_SUB_HEADER_PREFIX))
                    .filter(entry -> Objects.toString(entry.getValue(), "").length() <= EnturGooglePubSubConstants.GOOGLE_PUB_SUB_MAX_ATTR_LENGTH)
                    .forEach(entry -> pubSubAttributes.put(entry.getKey(), Objects.toString(entry.getValue())));
            pubSubTemplate.publish(endpoint.getDestinationName(), body, pubSubAttributes);
        }


    }

}






