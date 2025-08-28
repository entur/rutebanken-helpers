package org.rutebanken.helper.stopplace.changelog.kafka;

import java.util.HashSet;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelogListener;
import org.rutebanken.helper.stopplace.changelog.repository.StopPlaceRepository;
import org.rutebanken.irkalla.avro.EnumType;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.messaging.handler.annotation.Payload;

public class KafkaStopPlaceChangelog implements StopPlaceChangelog {

  public static final String CHANGELOG_LISTENER = "tiamatChangelogListener";

  private final Set<StopPlaceChangelogListener> listeners = new HashSet<>();

  private final StopPlaceRepository stopPlaceRepository;

  public KafkaStopPlaceChangelog(StopPlaceRepository stopPlaceRepository) {
    this.stopPlaceRepository = stopPlaceRepository;
  }

  @Override
  public void registerStopPlaceChangelogListener(
    StopPlaceChangelogListener listener
  ) {
    if (listener == null) {
      throw new IllegalArgumentException("listener must not be null");
    }
    listeners.add(listener);
  }

  @Override
  public void unregisterStopPlaceChangelogListener(
    StopPlaceChangelogListener listener
  ) {
    if (listener == null) {
      throw new IllegalArgumentException("listener must not be null");
    }
    listeners.remove(listener);
  }

  @KafkaListener(
    id = CHANGELOG_LISTENER,
    groupId = "${org.rutebanken.helper.stopplace.changelog.kafka.group-id:#{T(java.util.UUID).randomUUID().toString()}}",
    autoStartup = "${org.rutebanken.helper.stopplace.changelog.kafka.autostartup:true}",
    topicPartitions = @TopicPartition(
      topic = "${org.rutebanken.helper.stopplace.changelog.kafka.topic:}",
      partitions = "#{@stopPlaceChangelogPartitionFinder.partitions(\"${org.rutebanken.helper.stopplace.changelog.kafka.topic:}\")}",
      partitionOffsets = @PartitionOffset(partition = "*", initialOffset = "0")
    ),
    filter = "publicationTimeRecordFilterStrategy",
    containerFactory = "tiamatChangelogListenerContainerFactory"
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
