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

    public void createSubscriptionIfMissing(String destinationName) {

        if (autocreate) {
            try {
                pubSubAdmin.createTopic(destinationName);
                logger.debug("Created topic: " + destinationName);
            } catch (AlreadyExistsException e) {
                logger.trace("Did not create topic: " + destinationName + " ,as it already exists");
            }

            try {
                pubSubAdmin.createSubscription(destinationName, destinationName);
                logger.debug("Created subscription: " + destinationName);
            } catch (AlreadyExistsException e) {
                logger.trace("Did not create subscription: " + destinationName + " ,as it already exists");
            }
        }
    }
}
