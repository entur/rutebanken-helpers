package org.rutebanken.helper.stopplace.changelog.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.core.ConsumerFactory;

public class PartitionFinder {

  private final ConsumerFactory<String, ?> consumerFactory;

  public PartitionFinder(ConsumerFactory<String, ?> consumerFactory) {
    this.consumerFactory = consumerFactory;
  }

  public String[] partitions(String topic) {
    try (Consumer<String, ?> consumer = consumerFactory.createConsumer()) {
      return consumer
        .partitionsFor(topic)
        .stream()
        .map(pi -> "" + pi.partition())
        .toArray(String[]::new);
    }
  }
}
