package org.entur.pubsub.base;

import java.util.Map;

/**
 * Google PubSub message consumer.
 */
@FunctionalInterface
public interface EnturGooglePubSubConsumer {

    /**
     * Callback for PubSub message consumer, called upon message arrival.
     * @param content message content
     * @param headers message headers
     */
    void onMessage(byte[] content, Map<String, String> headers);
}
