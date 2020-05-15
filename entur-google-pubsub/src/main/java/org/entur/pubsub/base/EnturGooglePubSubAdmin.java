package org.entur.pubsub.base;

import com.google.api.gax.rpc.AlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.stereotype.Component;

@Component
public class EnturGooglePubSubAdmin {

    private static final Logger logger = LoggerFactory.getLogger(EnturGooglePubSubAdmin.class);

    @Autowired
    private PubSubAdmin pubSubAdmin;

    @Value("${entur.pubsub.subscriber.autocreate:true}")
    private boolean autocreate;

    /**
     * Creates a subscription to the given destinationName topic.
     * SubscriptionName will be equal to destinationName
     * @param destinationName This is the name of the topic to subscribe to, will also be used as subscriptionName
     */

    public void createSubscriptionIfMissing(String destinationName) {
        createSubscriptionIfMissing(destinationName, destinationName);
    }

    /**
     * Creates a subscription with the given subscriptionName to the given destinationName topic.
     * @param subscriptionName This is the name of the subscription that will be created/used
     * @param destinationName This is the name of the topic to subscribe to
     */

    public void createSubscriptionIfMissing(String destinationName, String subscriptionName) {
        if (autocreate) {
            try {
                pubSubAdmin.createTopic(destinationName);
                logger.debug("Created topic: {}", destinationName);
            } catch (AlreadyExistsException e) {
                logger.trace("Did not create topic: {}, as it already exists", destinationName);
            }

            try {
                pubSubAdmin.createSubscription(subscriptionName, destinationName);
                logger.debug("Created subscription: {}", destinationName);
            } catch (AlreadyExistsException e) {
                logger.trace("Did not create subscription: {}, as it already exists", destinationName);
            }
        }
    }
}
