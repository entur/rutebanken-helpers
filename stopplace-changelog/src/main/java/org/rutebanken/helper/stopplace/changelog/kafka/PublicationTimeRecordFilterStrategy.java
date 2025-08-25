package org.rutebanken.helper.stopplace.changelog.kafka;

import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

public class PublicationTimeRecordFilterStrategy
  implements RecordFilterStrategy<String, StopPlaceChangelogEvent> {

  private final Instant publicationTime;

  public PublicationTimeRecordFilterStrategy(Instant publicationTime) {
    this.publicationTime = publicationTime;
  }

  @Override
  public boolean filter(
    ConsumerRecord<String, StopPlaceChangelogEvent> consumerRecord
  ) {
    var changedTime = consumerRecord.value().getStopPlaceChanged();
    if (changedTime == null) {
      return true;
    }

    return changedTime.isBefore(publicationTime);
  }
}
