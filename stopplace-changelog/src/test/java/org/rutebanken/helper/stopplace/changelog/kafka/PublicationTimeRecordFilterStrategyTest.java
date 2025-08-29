package org.rutebanken.helper.stopplace.changelog.kafka;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.irkalla.avro.EnumType;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;

class PublicationTimeRecordFilterStrategyTest {

  private static final String STOP_PLACE_ID = "NSR:StopPlace:123";
  private static final String TOPIC = "stop-place-changelog";
  private Instant cutoffTime;
  private PublicationTimeRecordFilterStrategy filterStrategy;

  @BeforeEach
  void setUp() {
    cutoffTime = Instant.now();
    filterStrategy = new PublicationTimeRecordFilterStrategy(cutoffTime);
  }

  @Test
  void testFilterReturnsTrueForEventsBeforeCutoffTime() {
    // Event timestamp is 1 hour before cutoff time
    Instant eventTime = cutoffTime.minus(1, ChronoUnit.HOURS);

    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(eventTime)
      .setEventType(EnumType.CREATE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, event);

    // Should filter out (return true) events before cutoff time
    assertTrue(
      filterStrategy.filter(record),
      "Events before cutoff time should be filtered out"
    );
  }

  @Test
  void testFilterReturnsFalseForEventsAfterCutoffTime() {
    // Event timestamp is 1 hour after cutoff time
    Instant eventTime = cutoffTime.plus(1, ChronoUnit.HOURS);

    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(eventTime)
      .setEventType(EnumType.UPDATE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, event);

    // Should not filter out (return false) events after cutoff time
    assertFalse(
      filterStrategy.filter(record),
      "Events after cutoff time should not be filtered out"
    );
  }

  @Test
  void testFilterReturnsFalseForEventsAtExactCutoffTime() {
    // Event timestamp is exactly at cutoff time
    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(cutoffTime)
      .setEventType(EnumType.REMOVE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, event);

    // Should not filter out (return false) events at exact cutoff time
    assertFalse(
      filterStrategy.filter(record),
      "Events at exact cutoff time should not be filtered out"
    );
  }

  @Test
  void testFilterReturnsTrueForEventsWithNullTimestamp() {
    // Event with null timestamp
    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(null) // Explicitly null
      .setEventType(EnumType.DELETE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, event);

    // Should filter out (return true) events with null timestamp
    assertTrue(
      filterStrategy.filter(record),
      "Events with null timestamp should be filtered out"
    );
  }

  @Test
  void testFilterWithEpochTimeCutoff() {
    // Using epoch time as cutoff (accept all events)
    PublicationTimeRecordFilterStrategy epochFilter =
      new PublicationTimeRecordFilterStrategy(Instant.EPOCH);

    // Recent event (should not be filtered)
    Instant recentTime = Instant.now();
    StopPlaceChangelogEvent recentEvent = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(recentTime)
      .setEventType(EnumType.CREATE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> recentRecord =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, recentEvent);

    assertFalse(
      epochFilter.filter(recentRecord),
      "Recent events should not be filtered when using epoch as cutoff"
    );

    // Very old event from 1970 (should not be filtered)
    Instant oldTime = Instant.EPOCH.plus(1, ChronoUnit.DAYS);
    StopPlaceChangelogEvent oldEvent = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(oldTime)
      .setEventType(EnumType.UPDATE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> oldRecord =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, oldEvent);

    assertFalse(
      epochFilter.filter(oldRecord),
      "Events after epoch should not be filtered when using epoch as cutoff"
    );
  }

  @Test
  void testFilterWithFutureCutoff() {
    // Using future time as cutoff (filter all events)
    Instant futureTime = Instant.now().plus(365, ChronoUnit.DAYS);
    PublicationTimeRecordFilterStrategy futureFilter =
      new PublicationTimeRecordFilterStrategy(futureTime);

    // Current event (should be filtered)
    Instant now = Instant.now();
    StopPlaceChangelogEvent currentEvent = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(now)
      .setEventType(EnumType.CREATE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> currentRecord =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, currentEvent);

    assertTrue(
      futureFilter.filter(currentRecord),
      "Current events should be filtered when using future time as cutoff"
    );
  }

  @Test
  void testFilterWithMillisecondPrecision() {
    // Test with millisecond precision
    Instant baseTime = Instant.parse("2024-01-15T10:30:00.500Z");
    PublicationTimeRecordFilterStrategy preciseFilter =
      new PublicationTimeRecordFilterStrategy(baseTime);

    // Event 1ms before cutoff
    Instant beforeTime = Instant.parse("2024-01-15T10:30:00.499Z");
    StopPlaceChangelogEvent beforeEvent = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(beforeTime)
      .setEventType(EnumType.UPDATE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> beforeRecord =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, beforeEvent);

    assertTrue(
      preciseFilter.filter(beforeRecord),
      "Event 1ms before cutoff should be filtered"
    );

    // Event 1ms after cutoff
    Instant afterTime = Instant.parse("2024-01-15T10:30:00.501Z");
    StopPlaceChangelogEvent afterEvent = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(afterTime)
      .setEventType(EnumType.UPDATE)
      .build();

    ConsumerRecord<String, StopPlaceChangelogEvent> afterRecord =
      new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, afterEvent);

    assertFalse(
      preciseFilter.filter(afterRecord),
      "Event 1ms after cutoff should not be filtered"
    );
  }

  @Test
  void testFilterIndependentOfEventType() {
    // Test that filtering is independent of event type
    Instant afterCutoff = cutoffTime.plus(1, ChronoUnit.HOURS);

    for (EnumType eventType : EnumType.values()) {
      StopPlaceChangelogEvent event = StopPlaceChangelogEvent
        .newBuilder()
        .setStopPlaceId(STOP_PLACE_ID)
        .setStopPlaceVersion(1L)
        .setStopPlaceChanged(afterCutoff)
        .setEventType(eventType)
        .build();

      ConsumerRecord<String, StopPlaceChangelogEvent> record =
        new ConsumerRecord<>(TOPIC, 0, 0, STOP_PLACE_ID, event);

      assertFalse(
        filterStrategy.filter(record),
        "Event type " + eventType + " after cutoff should not be filtered"
      );
    }
  }

  @Test
  void testFilterIndependentOfPartitionAndOffset() {
    // Test that filtering is independent of partition and offset
    Instant afterCutoff = cutoffTime.plus(1, ChronoUnit.HOURS);

    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(STOP_PLACE_ID)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(afterCutoff)
      .setEventType(EnumType.CREATE)
      .build();

    // Test different partitions
    for (int partition = 0; partition < 10; partition++) {
      ConsumerRecord<String, StopPlaceChangelogEvent> record =
        new ConsumerRecord<>(TOPIC, partition, 100L, STOP_PLACE_ID, event);

      assertFalse(
        filterStrategy.filter(record),
        "Partition " + partition + " should not affect filtering"
      );
    }

    // Test different offsets
    for (long offset = 0L; offset < 1000L; offset += 100) {
      ConsumerRecord<String, StopPlaceChangelogEvent> record =
        new ConsumerRecord<>(TOPIC, 0, offset, STOP_PLACE_ID, event);

      assertFalse(
        filterStrategy.filter(record),
        "Offset " + offset + " should not affect filtering"
      );
    }
  }

  @Test
  void testPublicationTimeAccessor() {
    // Test that the publicationTime() method returns the correct value
    Instant expectedTime = Instant.parse("2024-06-15T12:00:00Z");
    PublicationTimeRecordFilterStrategy strategy =
      new PublicationTimeRecordFilterStrategy(expectedTime);

    assertEquals(
      expectedTime,
      strategy.publicationTime(),
      "publicationTime() should return the time passed to constructor"
    );
  }
}
