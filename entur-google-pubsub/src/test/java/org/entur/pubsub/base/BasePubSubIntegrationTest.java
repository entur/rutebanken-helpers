package org.entur.pubsub.base;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = TestApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BasePubSubIntegrationTest {

  private static PubSubEmulatorContainer pubsubEmulator;

  @Autowired
  private EnturGooglePubSubAdmin enturGooglePubSubAdmin;

  @BeforeAll
  static void init() {
    pubsubEmulator =
      new PubSubEmulatorContainer(
        DockerImageName.parse(
          "gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"
        )
      );
    pubsubEmulator.start();
  }

  @AfterAll
  static void tearDown() {
    pubsubEmulator.stop();
  }

  @AfterEach
  void teardown() {
    enturGooglePubSubAdmin.deleteAllSubscriptions();
  }

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.cloud.gcp.pubsub.emulator-host",
      pubsubEmulator::getEmulatorEndpoint
    );
    registry.add(
      "camel.component.google-pubsub.endpoint",
      pubsubEmulator::getEmulatorEndpoint
    );
  }
}
