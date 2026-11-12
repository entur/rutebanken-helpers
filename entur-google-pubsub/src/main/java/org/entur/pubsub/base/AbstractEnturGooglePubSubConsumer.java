package org.entur.pubsub.base;

import com.google.api.core.ApiService;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Base class for PubSub message consumers.
 * Consumers are started as late as possible after the Spring context initialization is complete,
 * and stopped as early as possible on context shutdown.
 */
@Component
public abstract class AbstractEnturGooglePubSubConsumer
  implements EnturGooglePubSubConsumer {

  @Autowired
  private EnturGooglePubSubAdmin enturGooglePubSubAdmin;

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Autowired
  private ApplicationContext applicationContext;

  @Value("${entur.pubsub.consumer.retry.delay:15000}")
  private long retryDelay;

  @Value("${entur.pubsub.consumer.break-liveness-on-terminal-failure:false}")
  private boolean breakLivenessOnTerminalFailure;

  private final AtomicReference<Throwable> terminalFailure =
    new AtomicReference<>();

  private volatile boolean closing;

  private volatile boolean ready;

  private final List<Subscriber> subscribers = new ArrayList<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(
    AbstractEnturGooglePubSubConsumer.class
  );

  protected abstract String getDestinationName();

  protected int getConcurrentConsumers() {
    return 1;
  }

  @EventListener
  public void handleContextRefreshed(
    ContextRefreshedEvent contextRefreshedEvent
  ) {
    // A separate management port gives the actuator a child context whose events reach the parent.
    if (contextRefreshedEvent.getApplicationContext() != applicationContext) {
      return;
    }
    LOGGER.info(
      "Initializing PubSub consumers for destination {}",
      getDestinationName()
    );

    enturGooglePubSubAdmin.createSubscriptionIfMissing(getDestinationName());

    Consumer<BasicAcknowledgeablePubsubMessage> messageConsumer =
      basicAcknowledgeablePubsubMessage -> {
        PubsubMessage pubsubMessage =
          basicAcknowledgeablePubsubMessage.getPubsubMessage();
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
            "Received message ID : {}",
            pubsubMessage.getMessageId()
          );
        }
        try {
          onMessage(
            pubsubMessage.getData().toByteArray(),
            pubsubMessage.getAttributesMap()
          );
          basicAcknowledgeablePubsubMessage.ack();
        } catch (Exception e) {
          basicAcknowledgeablePubsubMessage.nack();
          LOGGER.error(
            "Message processing failed, retrying in {} milliseconds",
            retryDelay,
            e
          );
          delay(retryDelay);
        }
      };
    for (int i = 0; i < getConcurrentConsumers(); i++) {
      Subscriber subscriber = pubSubTemplate.subscribe(
        getDestinationName(),
        messageConsumer
      );
      watchForTerminalFailure(subscriber);
      subscribers.add(subscriber);
    }

    LOGGER.info(
      "Initialized PubSub consumers for destination {}",
      getDestinationName()
    );
  }

  // A status the client does not retry leaves the subscriber FAILED for good, and nothing else notices.
  private void watchForTerminalFailure(Subscriber subscriber) {
    subscriber.addListener(
      new ApiService.Listener() {
        @Override
        public void failed(ApiService.State from, Throwable failure) {
          reportTerminalFailure(failure);
        }
      },
      Runnable::run
    );
  }

  /**
   * Spring Boot publishes {@code LivenessState.CORRECT} once the context has refreshed, so
   * escalating while subscribing is overwritten - which is when a missing or forbidden subscription
   * fails. The sweep also catches a subscriber that failed before its listener was registered.
   */
  @EventListener
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public final void handleApplicationReady(
    ApplicationReadyEvent applicationReadyEvent
  ) {
    subscribers
      .stream()
      .filter(subscriber -> subscriber.state() == ApiService.State.FAILED)
      .forEach(subscriber -> reportTerminalFailure(subscriber.failureCause()));

    ready = true;
    Throwable failure = terminalFailure.get();
    if (failure != null) {
      breakLiveness(failure);
    }
  }

  private void reportTerminalFailure(Throwable failure) {
    if (closing || !terminalFailure.compareAndSet(null, failure)) {
      return;
    }
    LOGGER.error(
      "PubSub subscriber for subscription {} failed permanently; nothing more will be consumed " +
      "from it until this pod is replaced. Check that the subscription exists and that the " +
      "service account has roles/pubsub.subscriber.",
      getDestinationName(),
      failure
    );
    breakLiveness(failure);
  }

  private void breakLiveness(Throwable failure) {
    if (ready && breakLivenessOnTerminalFailure) {
      AvailabilityChangeEvent.publish(
        applicationContext,
        failure,
        LivenessState.BROKEN
      );
    }
  }

  /**
   * Wait for the number of specified milliseconds.
   * @param delay delay in milliseconds
   */
  private static void delay(long delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EnturGooglePubSubException(e);
    }
  }

  @EventListener
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public void handleContextClosedEvent(ContextClosedEvent contextClosedEvent) {
    closing = true;
    LOGGER.info(
      "Stopping Google PubSub consumer for subscription {}",
      getDestinationName()
    );
    for (Subscriber subscriber : subscribers) {
      EnturGooglePubSubUtils.closeSubscriber(subscriber);
    }
    LOGGER.info(
      "Stopped Google PubSub consumer for subscription {}",
      getDestinationName()
    );
  }
}
