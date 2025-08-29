package org.entur.ror.helpers.stopplace.changelog.config;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelogListener;
import org.rutebanken.helper.stopplace.changelog.kafka.ChangelogConsumerController;
import org.rutebanken.helper.stopplace.changelog.repository.StopPlaceRepository;
import org.rutebanken.irkalla.avro.EnumType;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for the StopPlace Changelog starter configuration.
 * This test verifies that the Spring Boot auto-configuration properly sets up
 * all the necessary beans and that the Kafka listener works correctly.
 */
@SpringBootTest(classes = TestApplication.class)
@EmbeddedKafka(
  partitions = 3,
  topics = "test-stop-place-changelog",
  brokerProperties = {
    "listeners=PLAINTEXT://localhost:0",
    "port=0",
    "auto.create.topics.enable=true",
  }
)
@TestPropertySource(
  properties = {
    "org.rutebanken.helper.stopplace.changelog.enabled=true",
    "org.rutebanken.helper.stopplace.changelog.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "org.rutebanken.helper.stopplace.changelog.kafka.topic=test-stop-place-changelog",
    "org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-url=mock://test",
    "org.rutebanken.helper.stopplace.changelog.kafka.autostartup=true",
    "org.rutebanken.helper.stopplace.changelog.repository.url=http://localhost:8080/test",
  }
)
@DirtiesContext
class StopPlaceChangelogIntegrationTest {

  @Autowired
  private StopPlaceChangelog stopPlaceChangelog;

  @Autowired
  private ChangelogConsumerController changelogConsumerController;

  @MockBean
  private StopPlaceRepository stopPlaceRepository;

  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  @Autowired
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  @BeforeEach
  void setUp() {
    // Wait for the Kafka listener container to be ready
    await()
      .atMost(Duration.ofSeconds(10))
      .until(() ->
        kafkaListenerEndpointRegistry.getListenerContainer(
          "tiamatChangelogListener"
        ) !=
        null
      );
  }

  @Test
  void testContextLoads() {
    // Verify that all required beans are present
    assertNotNull(
      stopPlaceChangelog,
      "StopPlaceChangelog bean should be created"
    );
    assertNotNull(
      changelogConsumerController,
      "ChangelogConsumerController bean should be created"
    );
    assertNotNull(
      stopPlaceRepository,
      "StopPlaceRepository bean should be present"
    );
  }

  @Test
  void testKafkaListenerConfiguration() {
    // Verify that the Kafka listener is configured correctly
    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(
      "tiamatChangelogListener"
    );
    assertNotNull(
      listenerContainer,
      "Kafka listener container should be created"
    );
    assertTrue(
      listenerContainer.isRunning(),
      "Kafka listener should be running by default"
    );
  }

  @Test
  void testStopAndStartListener() {
    // Test that we can control the listener through the controller
    changelogConsumerController.stop();

    await()
      .atMost(Duration.ofSeconds(5))
      .until(() ->
        !kafkaListenerEndpointRegistry
          .getListenerContainer("tiamatChangelogListener")
          .isRunning()
      );

    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(
      "tiamatChangelogListener"
    );
    assertFalse(listenerContainer.isRunning(), "Listener should be stopped");

    changelogConsumerController.start();

    await()
      .atMost(Duration.ofSeconds(5))
      .until(() ->
        kafkaListenerEndpointRegistry
          .getListenerContainer("tiamatChangelogListener")
          .isRunning()
      );

    assertTrue(
      listenerContainer.isRunning(),
      "Listener should be running again"
    );
  }

  @Test
  void testEndToEndEventProcessing() throws Exception {
    // Setup
    String stopPlaceId = "NSR:StopPlace:123";
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> receivedId = new AtomicReference<>();
    AtomicReference<InputStream> receivedData = new AtomicReference<>();

    // Mock repository to return test data
    byte[] testXmlData =
      "<?xml version=\"1.0\"?><StopPlace>test</StopPlace>".getBytes();
    when(stopPlaceRepository.getStopPlaceUpdate(stopPlaceId))
      .thenReturn(new ByteArrayInputStream(testXmlData));

    // Register a listener
    StopPlaceChangelogListener listener = new StopPlaceChangelogListener() {
      @Override
      public void onStopPlaceCreated(String id, InputStream stopPlace) {
        receivedId.set(id);
        receivedData.set(stopPlace);
        latch.countDown();
      }

      @Override
      public void onStopPlaceUpdated(String id, InputStream stopPlace) {}

      @Override
      public void onStopPlaceDeactivated(String id, InputStream stopPlace) {}

      @Override
      public void onStopPlaceDeleted(String id) {}
    };

    stopPlaceChangelog.registerStopPlaceChangelogListener(listener);

    // Wait for listener to be ready
    Thread.sleep(2000);

    // Produce an event
    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(stopPlaceId)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(Instant.now())
      .setEventType(EnumType.CREATE)
      .build();

    KafkaProducer<String, StopPlaceChangelogEvent> producer = createProducer();
    producer
      .send(
        new ProducerRecord<>("test-stop-place-changelog", stopPlaceId, event)
      )
      .get();
    producer.close();

    // Verify the event was processed
    assertTrue(
      latch.await(10, TimeUnit.SECONDS),
      "Event should be processed within 10 seconds"
    );
    assertEquals(
      stopPlaceId,
      receivedId.get(),
      "Correct stop place ID should be received"
    );
    assertNotNull(receivedData.get(), "Stop place data should be received");

    // Verify repository was called
    verify(stopPlaceRepository, timeout(5000)).getStopPlaceUpdate(stopPlaceId);

    // Cleanup
    stopPlaceChangelog.unregisterStopPlaceChangelogListener(listener);
  }

  @Test
  void testMultipleListeners() throws Exception {
    // Setup
    String stopPlaceId = "NSR:StopPlace:456";
    CountDownLatch latch = new CountDownLatch(2);
    AtomicReference<Integer> callCount = new AtomicReference<>(0);

    // Create two listeners
    StopPlaceChangelogListener listener1 = new StopPlaceChangelogListener() {
      @Override
      public void onStopPlaceDeleted(String id) {
        assertEquals(stopPlaceId, id);
        callCount.updateAndGet(v -> v + 1);
        latch.countDown();
      }
    };

    StopPlaceChangelogListener listener2 = new StopPlaceChangelogListener() {
      @Override
      public void onStopPlaceDeleted(String id) {
        assertEquals(stopPlaceId, id);
        callCount.updateAndGet(v -> v + 1);
        latch.countDown();
      }
    };

    // Register both listeners
    stopPlaceChangelog.registerStopPlaceChangelogListener(listener1);
    stopPlaceChangelog.registerStopPlaceChangelogListener(listener2);

    // Wait for listeners to be ready
    Thread.sleep(2000);

    // Produce a DELETE event
    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(stopPlaceId)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(Instant.now())
      .setEventType(EnumType.DELETE)
      .build();

    KafkaProducer<String, StopPlaceChangelogEvent> producer = createProducer();
    producer
      .send(
        new ProducerRecord<>("test-stop-place-changelog", stopPlaceId, event)
      )
      .get();
    producer.close();

    // Both listeners should receive the event
    assertTrue(
      latch.await(10, TimeUnit.SECONDS),
      "Both listeners should receive the event"
    );
    assertEquals(2, callCount.get(), "Both listeners should have been called");

    // Cleanup
    stopPlaceChangelog.unregisterStopPlaceChangelogListener(listener1);
    stopPlaceChangelog.unregisterStopPlaceChangelogListener(listener2);
  }

  @Test
  void testEventFiltering() throws Exception {
    // Test that old events are filtered out
    String stopPlaceId = "NSR:StopPlace:789";
    CountDownLatch oldEventLatch = new CountDownLatch(1);
    CountDownLatch newEventLatch = new CountDownLatch(1);

    StopPlaceChangelogListener listener = new StopPlaceChangelogListener() {
      @Override
      public void onStopPlaceDeleted(String id) {
        if ("NSR:StopPlace:789".equals(id)) {
          oldEventLatch.countDown();
        } else if ("NSR:StopPlace:790".equals(id)) {
          newEventLatch.countDown();
        }
      }
    };

    stopPlaceChangelog.registerStopPlaceChangelogListener(listener);

    // Wait for listener to be ready
    Thread.sleep(2000);

    // Create an old event (should be filtered)
    Instant oldTime = Instant.now().minusSeconds(3600); // 1 hour ago
    StopPlaceChangelogEvent oldEvent = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(stopPlaceId)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(oldTime)
      .setEventType(EnumType.DELETE)
      .build();

    // Create a new event (should not be filtered)
    String newStopPlaceId = "NSR:StopPlace:790";
    Instant newTime = Instant.now().plusSeconds(60); // 1 minute in the future
    StopPlaceChangelogEvent newEvent = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId(newStopPlaceId)
      .setStopPlaceVersion(1L)
      .setStopPlaceChanged(newTime)
      .setEventType(EnumType.DELETE)
      .build();

    KafkaProducer<String, StopPlaceChangelogEvent> producer = createProducer();
    producer
      .send(
        new ProducerRecord<>("test-stop-place-changelog", stopPlaceId, oldEvent)
      )
      .get();
    producer
      .send(
        new ProducerRecord<>(
          "test-stop-place-changelog",
          newStopPlaceId,
          newEvent
        )
      )
      .get();
    producer.close();

    // The new event should be processed, but not the old one
    assertTrue(
      newEventLatch.await(10, TimeUnit.SECONDS),
      "New event should be processed"
    );
    assertFalse(
      oldEventLatch.await(2, TimeUnit.SECONDS),
      "Old event should be filtered out"
    );

    // Cleanup
    stopPlaceChangelog.unregisterStopPlaceChangelogListener(listener);
  }

  private KafkaProducer<String, StopPlaceChangelogEvent> createProducer() {
    Map<String, Object> props = new HashMap<>();
    props.put(
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
      embeddedKafkaBroker.getBrokersAsString()
    );
    props.put(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      StringSerializer.class
    );
    props.put(
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      KafkaAvroSerializer.class
    );
    props.put("schema.registry.url", "mock://test");
    return new KafkaProducer<>(props);
  }
}
