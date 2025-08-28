package org.rutebanken.helper.stopplace.changelog.kafka;

import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * Filter StopPlaceChangelogEvent based on its publication time.
 */
public abstract class BasePublicationTimeRecordFilterStrategy
  implements RecordFilterStrategy<String, StopPlaceChangelogEvent> {

  @Override
  public boolean filter(
    ConsumerRecord<String, StopPlaceChangelogEvent> consumerRecord
  ) {
    var changedTime = consumerRecord.value().getStopPlaceChanged();
    if (changedTime == null) {
      return true;
    }

    return changedTime.isBefore(publicationTime());
  }

  protected abstract Instant publicationTime();
}
