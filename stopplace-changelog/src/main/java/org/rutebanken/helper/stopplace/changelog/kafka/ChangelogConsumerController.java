package org.rutebanken.helper.stopplace.changelog.kafka;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

/**
 * Control the lifecycle of the Kafka consumer.
 */
public class ChangelogConsumerController {

  private final KafkaListenerEndpointRegistry registry;

  public ChangelogConsumerController(KafkaListenerEndpointRegistry registry) {
    this.registry = registry;
  }

  /**
   * Start the Kafka consumer.
   * This resets the message index to 0.
   */
  public void start() {
    registry
      .getListenerContainer(KafkaStopPlaceChangelog.CHANGELOG_LISTENER)
      .start();
  }

  /**
   * Stop the Kafka consumer.
   */
  public void stop() {
    registry
      .getListenerContainer(KafkaStopPlaceChangelog.CHANGELOG_LISTENER)
      .stop();
  }

  /**
   *
   * Return true if the Kafka consumer is running.
   */
  public boolean isRunning() {
    return registry
      .getListenerContainer(KafkaStopPlaceChangelog.CHANGELOG_LISTENER)
      .isRunning();
  }
}
