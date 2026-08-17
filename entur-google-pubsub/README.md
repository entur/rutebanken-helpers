# entur-google-pubsub

Base configuration and utilities for Google Cloud Pub/Sub integration in Spring Boot applications. Provides consumer abstractions and admin utilities built on top of Spring Cloud GCP.

> For Apache Camel-based Pub/Sub integration, use `camel-entur-google-pubsub` instead.

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>entur-google-pubsub</artifactId>
    <version>VERSION</version>
</dependency>
```

## Wiring

Import the configuration on your application class; it component-scans the consumers and admin.

```java
@SpringBootApplication
@Import(GooglePubSubConfig.class)
public class App { }
```

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

Extend `AbstractEnturGooglePubSubConsumer` and name the subscription. Returning normally acks the
message; throwing nacks it, and Pub/Sub redelivers.

```java
@Component
public class MyMessageConsumer extends AbstractEnturGooglePubSubConsumer {

    @Override
    protected String getDestinationName() {
        return "MyQueue";
    }

    @Override
    public void onMessage(byte[] content, Map<String, String> attributes) {
        // process content; throw to nack
    }
}
```

For admin operations (e.g., in integration tests or setup scripts):

```java
@Autowired
private EnturGooglePubSubAdmin pubSubAdmin;

// creates the topic and a subscription of the same name, unless
// entur.pubsub.subscriber.autocreate is false
pubSubAdmin.createSubscriptionIfMissing("my-destination");
```

## Failure handling

| Property | Default | Effect |
|---|---|---|
| `entur.pubsub.subscriber.autocreate` | `true` | Creates the topic **and** the subscription on startup. Set it to `false` wherever they are terraformed: the topic is created first and unconditionally, so a service account holding only `roles/pubsub.subscriber` fails context refresh. |
| `entur.pubsub.consumer.retry.delay` | `15000` | How long the callback thread sleeps after a nack. It does not delay redelivery, which the subscription's `retry_policy` governs. |
| `entur.pubsub.consumer.break-liveness-on-terminal-failure` | `false` | See below. |

A status the client does not retry - NOT_FOUND, PERMISSION_DENIED and UNAUTHENTICATED among them -
leaves the subscriber `FAILED` for good. It never restarts, and the subscription silently stops being
served while the application keeps reporting healthy.

A terminal failure is logged once per consumer bean at ERROR, on the
`org.entur.pubsub.base.AbstractEnturGooglePubSubConsumer` logger. That line is the signal; alert on
it. Note it is the base class logger, so a filter keyed on your subclass name will not match.

`break-liveness-on-terminal-failure=true` additionally publishes
`AvailabilityChangeEvent(LivenessState.BROKEN)`, so `/actuator/health/liveness` reports DOWN and the
platform restarts the pod. It is off by default: a restart cannot recreate a subscription that
autocreate is not permitted to create, so NOT_FOUND and PERMISSION_DENIED only crash-loop, and in an
application that serves anything else they take that down too. It is also a whole-application
switch, and a no-op unless the liveness probe actually reads `/actuator/health/liveness`. Escalation
is one-way: nothing republishes CORRECT.


---

[Back to root](../README.md)