# entur-google-pubsub

Base configuration and utilities for Google Cloud Pub/Sub integration in Spring Boot applications. Provides auto-configuration, consumer abstractions, and admin utilities built on top of Spring Cloud GCP.

> For Apache Camel-based Pub/Sub integration, use `camel-entur-google-pubsub` instead.

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>entur-google-pubsub</artifactId>
    <version>VERSION</version>
</dependency>
```

## Auto-Configuration

`GooglePubSubConfig` is registered as a Spring Boot auto-configuration and runs before `GcpPubSubAutoConfiguration`. It is activated automatically when the library is on the classpath — no explicit `@Import` needed.

## Key Classes

| Class | Description |
|-------|-------------|
| `EnturGooglePubSubConsumer` | Interface for implementing a Pub/Sub message consumer |
| `AbstractEnturGooglePubSubConsumer` | Base class with common consumer logic |
| `EnturGooglePubSubAdmin` | Admin operations: create/delete topics and subscriptions |
| `EnturGooglePubSubUtils` | Utility methods for Pub/Sub operations |
| `EnturGooglePubSubException` | Custom exception for Pub/Sub errors |

## Configuration

Standard Spring Cloud GCP Pub/Sub configuration applies:

```yaml
spring:
  cloud:
    gcp:
      project-id: my-gcp-project
      pubsub:
        project-id: my-gcp-project   # overrides above if needed
        # credentials:
        #   location: classpath:service-account.json
```

## Usage

Implement `EnturGooglePubSubConsumer` or extend `AbstractEnturGooglePubSubConsumer` to process incoming messages:

```java
@Component
public class MyMessageConsumer extends AbstractEnturGooglePubSubConsumer {

    @Override
    protected void processMessage(BasicAcknowledgeablePubsubMessage message) {
        String payload = message.getPubsubMessage().getData().toStringUtf8();
        // process payload
        message.ack();
    }
}
```

For admin operations (e.g., in integration tests or setup scripts):

```java
@Autowired
private EnturGooglePubSubAdmin pubSubAdmin;

pubSubAdmin.createTopicIfNotExists("my-topic");
pubSubAdmin.createSubscriptionIfNotExists("my-subscription", "my-topic");
```

---

[Back to root](../README.md)