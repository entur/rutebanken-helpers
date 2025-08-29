package org.rutebanken.helper.stopplace.changelog.kafka;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelogListener;
import org.rutebanken.helper.stopplace.changelog.repository.StopPlaceRepository;
import org.rutebanken.irkalla.avro.EnumType;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;

@ExtendWith(MockitoExtension.class)
class KafkaStopPlaceChangelogTest {

  @Mock
  private StopPlaceRepository stopPlaceRepository;

  @Mock
  private StopPlaceChangelogListener listener;

  private KafkaStopPlaceChangelog changelog;

  @BeforeEach
  void setUp() {
    changelog = new KafkaStopPlaceChangelog(stopPlaceRepository);
  }

  @Test
  void testRegisterListener() {
    changelog.registerStopPlaceChangelogListener(listener);

    // Trigger an event to verify listener was registered
    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.DELETE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    verify(listener).onStopPlaceDeleted("NSR:StopPlace:1");
  }

  @Test
  void testRegisterNullListenerThrowsException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> changelog.registerStopPlaceChangelogListener(null)
    );
  }

  @Test
  void testUnregisterListener() {
    changelog.registerStopPlaceChangelogListener(listener);
    changelog.unregisterStopPlaceChangelogListener(listener);

    // Trigger an event to verify listener was unregistered
    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.DELETE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    verify(listener, never()).onStopPlaceDeleted(anyString());
  }

  @Test
  void testUnregisterNullListenerThrowsException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> changelog.unregisterStopPlaceChangelogListener(null)
    );
  }

  @Test
  void testConsumeCreateEvent() {
    InputStream mockData = new ByteArrayInputStream(
      "<xml>test</xml>".getBytes()
    );
    when(stopPlaceRepository.getStopPlaceUpdate("NSR:StopPlace:1"))
      .thenReturn(mockData);

    changelog.registerStopPlaceChangelogListener(listener);

    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.CREATE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    verify(stopPlaceRepository).getStopPlaceUpdate("NSR:StopPlace:1");
    verify(listener)
      .onStopPlaceCreated(eq("NSR:StopPlace:1"), any(InputStream.class));
  }

  @Test
  void testConsumeUpdateEvent() {
    InputStream mockData = new ByteArrayInputStream(
      "<xml>test</xml>".getBytes()
    );
    when(stopPlaceRepository.getStopPlaceUpdate("NSR:StopPlace:1"))
      .thenReturn(mockData);

    changelog.registerStopPlaceChangelogListener(listener);

    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.UPDATE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    verify(stopPlaceRepository).getStopPlaceUpdate("NSR:StopPlace:1");
    verify(listener)
      .onStopPlaceUpdated(eq("NSR:StopPlace:1"), any(InputStream.class));
  }

  @Test
  void testConsumeRemoveEvent() {
    InputStream mockData = new ByteArrayInputStream(
      "<xml>test</xml>".getBytes()
    );
    when(stopPlaceRepository.getStopPlaceUpdate("NSR:StopPlace:1"))
      .thenReturn(mockData);

    changelog.registerStopPlaceChangelogListener(listener);

    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.REMOVE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    verify(stopPlaceRepository).getStopPlaceUpdate("NSR:StopPlace:1");
    verify(listener)
      .onStopPlaceDeactivated(eq("NSR:StopPlace:1"), any(InputStream.class));
  }

  @Test
  void testConsumeDeleteEvent() {
    changelog.registerStopPlaceChangelogListener(listener);

    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.DELETE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    verify(stopPlaceRepository, never()).getStopPlaceUpdate(anyString());
    verify(listener).onStopPlaceDeleted("NSR:StopPlace:1");
  }

  @Test
  void testMultipleListeners() {
    StopPlaceChangelogListener listener2 = mock(
      StopPlaceChangelogListener.class
    );

    changelog.registerStopPlaceChangelogListener(listener);
    changelog.registerStopPlaceChangelogListener(listener2);

    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.DELETE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    verify(listener).onStopPlaceDeleted("NSR:StopPlace:1");
    verify(listener2).onStopPlaceDeleted("NSR:StopPlace:1");
  }

  @Test
  void testAllListenersExecute() {
    AtomicInteger counter = new AtomicInteger(0);

    StopPlaceChangelogListener orderListener1 =
      new StopPlaceChangelogListener() {
        @Override
        public void onStopPlaceDeleted(String id) {
          counter.incrementAndGet();
        }
      };

    StopPlaceChangelogListener orderListener2 =
      new StopPlaceChangelogListener() {
        @Override
        public void onStopPlaceDeleted(String id) {
          counter.incrementAndGet();
        }
      };

    changelog.registerStopPlaceChangelogListener(orderListener1);
    changelog.registerStopPlaceChangelogListener(orderListener2);

    StopPlaceChangelogEvent event = createEvent(
      "NSR:StopPlace:1",
      EnumType.DELETE
    );
    ConsumerRecord<String, StopPlaceChangelogEvent> record =
      new ConsumerRecord<>("topic", 0, 0, "key", event);

    changelog.consume(record);

    assertEquals(2, counter.get(), "Both listeners should have executed");
  }

  private StopPlaceChangelogEvent createEvent(
    String stopPlaceId,
    EnumType eventType
  ) {
    return StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(stopPlaceId)
      .setStopPlaceVersion(1L)
      .setEventType(eventType)
      .build();
  }
}
