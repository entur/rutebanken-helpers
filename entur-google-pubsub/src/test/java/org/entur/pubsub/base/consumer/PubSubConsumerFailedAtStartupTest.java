package org.entur.pubsub.base.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.core.ApiService;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.entur.pubsub.base.AbstractEnturGooglePubSubConsumer;
import org.entur.pubsub.base.EnturGooglePubSubConsumer;
import org.entur.pubsub.base.TestApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// The subscriber is already FAILED when subscribe() returns, so only the ApplicationReadyEvent
// sweep can notice it. See handleApplicationReady for why escalation cannot happen any earlier.
@SpringBootTest(
  classes = TestApp.class,
  properties = {
    "entur.pubsub.subscriber.autocreate=false",
    "entur.pubsub.consumer.break-liveness-on-terminal-failure=true",
    // Selects the no-credentials code path; nothing connects, because the stub replaces the
    // template and autocreate is off.
    "spring.cloud.gcp.pubsub.emulator-host=localhost:8085",
  }
)
@Import(PubSubConsumerFailedAtStartupTest.FailedAtStartupConfig.class)
class PubSubConsumerFailedAtStartupTest {

  private static final List<LivenessState> LIVENESS =
    new CopyOnWriteArrayList<>();

  @TestConfiguration
  static class FailedAtStartupConfig {

    @Bean
    PubSubTemplate pubSubTemplate() {
      Subscriber subscriber = mock(Subscriber.class);
      when(subscriber.state()).thenReturn(ApiService.State.FAILED);
      when(subscriber.failureCause())
        .thenReturn(new IllegalStateException("subscription does not exist"));

      PubSubTemplate pubSubTemplate = mock(PubSubTemplate.class);
      when(pubSubTemplate.subscribe(anyString(), any())).thenReturn(subscriber);
      return pubSubTemplate;
    }

    @Bean
    EnturGooglePubSubConsumer consumer() {
      return new AbstractEnturGooglePubSubConsumer() {
        @Override
        protected String getDestinationName() {
          return "AnyQueue";
        }

        @Override
        public void onMessage(byte[] content, Map<String, String> attributes) {
          // never called
        }
      };
    }

    @Bean
    ApplicationListener<AvailabilityChangeEvent<LivenessState>> livenessRecorder() {
      return event -> LIVENESS.add(event.getState());
    }
  }

  @Test
  void brokenLivenessSurvivesStartup() {
    assertTrue(
      LIVENESS.contains(LivenessState.CORRECT),
      "guard: CORRECT was published"
    );
    assertEquals(
      LivenessState.BROKEN,
      LIVENESS.get(LIVENESS.size() - 1),
      "BROKEN must be last"
    );
  }
}
