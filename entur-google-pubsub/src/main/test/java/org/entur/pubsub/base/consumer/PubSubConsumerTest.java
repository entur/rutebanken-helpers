package org.entur.pubsub.base.consumer;

import org.entur.pubsub.base.AbstractEnturGooglePubSubConsumer;
import org.entur.pubsub.base.BasePubSubIntegrationTest;
import org.entur.pubsub.base.EnturGooglePubSubConsumer;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.context.annotation.Bean;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class PubSubConsumerTest extends BasePubSubIntegrationTest {

    public static final String TEST_QUEUE = "TestQueue";
    public static final String TEST_PAYLOAD = "Test Payload";

    @Autowired
    private PubSubTemplate pubSubTemplate;

    private static CompletableFuture<String> messageContent = new CompletableFuture<>();


    @TestConfiguration
    static class EnturGooglePubSubConsumerTestConfiguration {

        @Bean
        public EnturGooglePubSubConsumer pubSubConsumer() {
            return new AbstractEnturGooglePubSubConsumer() {
                @Override
                protected String getDestinationName() {
                    return TEST_QUEUE;
                }

                @Override
                public void onMessage(byte[] content, Map<String, String> headers) {
                    messageContent.complete(new String(content));
                }
            };
        }
    }

    @Test
    public void testMessageConsumer() throws InterruptedException, ExecutionException, TimeoutException {

        pubSubTemplate.publish(TEST_QUEUE, TEST_PAYLOAD);
        Assert.assertEquals(messageContent.get(10, TimeUnit.SECONDS), TEST_PAYLOAD);

    }

}
