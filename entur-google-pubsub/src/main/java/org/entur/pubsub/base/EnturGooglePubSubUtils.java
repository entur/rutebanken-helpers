package org.entur.pubsub.base;

import com.google.cloud.pubsub.v1.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class for PubSub operations.
 */
public final class EnturGooglePubSubUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnturGooglePubSubUtils.class);

    private EnturGooglePubSubUtils() {
    }

    /**
     * Close a subscriber and release the associated resources.
     * Wait (10s max) for the subscriber to shut down before returning.
     *
     * @param subscriber the subscriber to close.
     */
    public static void closeSubscriber(Subscriber subscriber) {

        String destinationName = subscriber.getSubscriptionNameString();
        LOGGER.trace("Stopping subscriber for {}", destinationName);
        try {
            subscriber.stopAsync().awaitTerminated(10, TimeUnit.SECONDS);
            LOGGER.trace("Stopped subscriber for {}", destinationName);
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout while trying to stop subscriber for {}", destinationName, e);
        } catch (IllegalStateException e) {
            LOGGER.warn("Failed to stop subscriber for {}", destinationName, subscriber.failureCause());
        }
    }

}
