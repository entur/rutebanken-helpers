# slack

Sends messages to a Slack channel via an Incoming Webhook URL.

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>slack</artifactId>
    <version>VERSION</version>
</dependency>
```

## Configuration

| Property | Required | Description |
|----------|----------|-------------|
| `helper.slack.endpoint` | Yes | Slack Incoming Webhook URL |

```yaml
helper:
  slack:
    endpoint: https://hooks.slack.com/services/T.../B.../...
```

## Key Classes

### `SlackPostService`

Spring component that posts messages to Slack.

| Method | Returns | Description |
|--------|---------|-------------|
| `publish(String messageText)` | `boolean` | Post a plain text message |
| `publish(SlackPayload payload)` | `boolean` | Post a structured payload |

Returns `true` on success, `false` on failure (errors are logged, not thrown).

### `SlackPayload`

Wraps the message text for structured posting. Can be extended for richer Slack Block Kit formatting.

## Usage

```java
@Service
public class AlertService {

    @Autowired
    private SlackPostService slackPostService;

    public void sendAlert(String message) {
        boolean sent = slackPostService.publish("Alert: " + message);
        if (!sent) {
            log.warn("Failed to send Slack notification");
        }
    }
}
```

---

[Back to root](../README.md)