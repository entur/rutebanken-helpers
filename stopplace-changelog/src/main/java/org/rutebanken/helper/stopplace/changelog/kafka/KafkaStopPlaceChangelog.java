package org.rutebanken.helper.stopplace.changelog.kafka;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelogListener;
import org.rutebanken.helper.stopplace.changelog.repository.StopPlaceRepository;
import org.rutebanken.irkalla.avro.EnumType;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
  value = "org.rutebanken.helper.stopplace.changelog.kafka",
  havingValue = "true"
)
public class KafkaStopPlaceChangelog implements StopPlaceChangelog {

  private final Set<StopPlaceChangelogListener> listeners = new HashSet<>();

  private final StopPlaceRepository stopPlaceRepository;

  public KafkaStopPlaceChangelog(StopPlaceRepository stopPlaceRepository) {
    this.stopPlaceRepository = stopPlaceRepository;
  }

  @Bean("publicationTimeRecordFilterStrategy")
  public RecordFilterStrategy<String, StopPlaceChangelogEvent> recordFilterStrategy() {
    return new PublicationTimeRecordFilterStrategy(Instant.now());
  }

  @Override
  public void registerStopPlaceChangelogListener(
    StopPlaceChangelogListener listener
  ) {
    listeners.add(listener);
  }

  @Override
  public void unregisterStopPlaceChangelogListener(
    StopPlaceChangelogListener listener
  ) {
    listeners.remove(listener);
  }

  @KafkaListener(
    topicPartitions = @TopicPartition(
      topic = "${org.rutebanken.helper.stopplace.changelog.kafka.topic:ror-stop-place-changelog-dev}",
      partitions = "#{@finder.partitions(\"${org.rutebanken.helper.stopplace.changelog.kafka.topic:ror-stop-place-changelog-dev}\")}",
      partitionOffsets = @PartitionOffset(partition = "*", initialOffset = "0")
    ),
    filter = "publicationTimeRecordFilterStrategy"
  )
  public void consume(
    @Payload ConsumerRecord<String, StopPlaceChangelogEvent> message
  ) {
    var event = message.value();

    String stopPlaceId = event.getStopPlaceId().toString();

    if (event.getEventType().equals(EnumType.DELETE)) {
      listeners.forEach(l -> l.onStopPlaceDeleted(stopPlaceId));
    } else {
      var update = stopPlaceRepository.getStopPlaceUpdate(stopPlaceId);
      if (event.getEventType().equals(EnumType.CREATE)) {
        listeners.forEach(l -> l.onStopPlaceCreated(stopPlaceId, update));
      } else if (event.getEventType().equals(EnumType.UPDATE)) {
        listeners.forEach(l -> l.onStopPlaceUpdated(stopPlaceId, update));
      } else if (event.getEventType().equals(EnumType.REMOVE)) {
        listeners.forEach(l -> l.onStopPlaceDeactivated(stopPlaceId, update));
      }
    }
  }
}
