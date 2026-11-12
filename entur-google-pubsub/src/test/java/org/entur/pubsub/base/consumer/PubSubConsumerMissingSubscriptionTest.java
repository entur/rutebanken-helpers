package org.entur.pubsub.base.consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.api.gax.rpc.NotFoundException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.entur.pubsub.base.AbstractEnturGooglePubSubConsumer;
import org.entur.pubsub.base.BasePubSubIntegrationTest;
import org.entur.pubsub.base.EnturGooglePubSubConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

// A subscription that does not exist gives NOT_FOUND, which the client does not retry, so the
// subscriber ends up FAILED and its listener fires.
@TestPropertySource(
  properties = {
    "entur.pubsub.subscriber.autocreate=false",
    "entur.pubsub.consumer.break-liveness-on-terminal-failure=true",
  }
)
@Import(PubSubConsumerMissingSubscriptionTest.MissingSubscriptionConfig.class)
class PubSubConsumerMissingSubscriptionTest extends BasePubSubIntegrationTest {

  private static final List<AvailabilityChangeEvent<LivenessState>> LIVENESS =
    new CopyOnWriteArrayList<>();

  @TestConfiguration
  static class MissingSubscriptionConfig {

    @Bean
    public EnturGooglePubSubConsumer failingPubSubConsumer() {
      return new AbstractEnturGooglePubSubConsumer() {
        @Override
        protected String getDestinationName() {
          return "NoSuchQueue";
        }

        @Override
        public void onMessage(byte[] content, Map<String, String> headers) {
          // never called
        }
      };
    }

    @Bean
    ApplicationListener<AvailabilityChangeEvent<LivenessState>> livenessRecorder() {
      return LIVENESS::add;
    }
  }

  @Test
  void aTerminallyFailedSubscriberBreaksLivenessAndStaysBroken() {
    await()
      .alias("liveness reported BROKEN")
      .atMost(Duration.ofSeconds(30))
      .until(() ->
        LIVENESS.stream().anyMatch(e -> e.getState() == LivenessState.BROKEN)
      );

    AvailabilityChangeEvent<LivenessState> broken = LIVENESS
      .stream()
      .filter(e -> e.getState() == LivenessState.BROKEN)
      .findFirst()
      .orElseThrow();

    assertInstanceOf(NotFoundException.class, broken.getSource());
  }
}
