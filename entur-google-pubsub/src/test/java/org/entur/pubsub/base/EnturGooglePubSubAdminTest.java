package org.entur.pubsub.base;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.pubsub.v1.Subscription;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EnturGooglePubSubAdminTest extends BasePubSubIntegrationTest {

  public static final Set<String> TEST_SUBSCRIPTIONS = Set.of(
    "test-subscription-1",
    "test-subscription-2"
  );

  @Autowired
  private EnturGooglePubSubAdmin enturGooglePubSubAdmin;

  @Autowired
  private PubSubAdmin pubSubAdmin;

  @Test
  void testCreateSubscriptionIfMissing() {
    TEST_SUBSCRIPTIONS.forEach(s ->
      enturGooglePubSubAdmin.createSubscriptionIfMissing(s)
    );
    Set<String> createdSubscriptions = pubSubAdmin
      .listSubscriptions()
      .stream()
      .map(Subscription::getName)
      .map(s -> StringUtils.substringAfterLast(s, "/"))
      .collect(Collectors.toSet());
    assertTrue(createdSubscriptions.containsAll(TEST_SUBSCRIPTIONS));
  }

  @Test
  void testDeleteAllSubscriptions() {
    TEST_SUBSCRIPTIONS.forEach(s ->
      enturGooglePubSubAdmin.createSubscriptionIfMissing(s)
    );
    enturGooglePubSubAdmin.deleteAllSubscriptions();
    assertTrue(pubSubAdmin.listSubscriptions().isEmpty());
  }
}
