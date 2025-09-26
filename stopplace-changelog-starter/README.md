# StopPlace Changelog Starter

A Spring Boot starter for consuming stop place change events from Kafka. This starter provides auto-configuration and simplified setup for listening to stop place lifecycle events (create, update, deactivate, delete) in your Spring Boot applications.

## Installation

Add the starter dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>stopplace-changelog-starter</artifactId>
    <version>5.45.0-SNAPSHOT</version>
</dependency>
```

This starter automatically includes the core `stopplace-changelog` library and provides Spring Boot auto-configuration.

## Quick Start

### 1. Add Configuration Properties

```properties
# Enable the stopplace changelog
org.rutebanken.helper.stopplace.changelog.enabled=true

# Kafka broker configuration
org.rutebanken.helper.stopplace.changelog.kafka.bootstrap-servers=localhost:9092
org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-url=http://localhost:8081
org.rutebanken.helper.stopplace.changelog.kafka.topic=stopplace-changelog

# Stop place repository URL for fetching stop place data
org.rutebanken.helper.stopplace.changelog.repository.url=https://api.entur.io/stop-places/v1
```

### 2. Create a Listener

```java
@Component
public class MyStopPlaceHandler implements StopPlaceChangelogListener {
    
    private static final Logger log = LoggerFactory.getLogger(MyStopPlaceHandler.class);
    
    @Override
    public void onStopPlaceCreated(String id, InputStream stopPlace) {
        log.info("Stop place created: {}", id);
        // Process the stop place data from the InputStream
    }
    
    @Override
    public void onStopPlaceUpdated(String id, InputStream stopPlace) {
        log.info("Stop place updated: {}", id);
        // Process the updated stop place data
    }
    
    @Override
    public void onStopPlaceDeactivated(String id, InputStream stopPlace) {
        log.info("Stop place deactivated: {}", id);
        // Handle deactivation
    }
    
    @Override
    public void onStopPlaceDeleted(String id) {
        log.info("Stop place deleted: {}", id);
        // Handle deletion (no stop place data available)
    }
}
```

### 3. Register Your Listener

```java
@Component
public class StopPlaceConfiguration {
    
    @Autowired
    private StopPlaceChangelog stopPlaceChangelog;
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        // Register all StopPlaceChangelogListener beans
        var listeners = event.getApplicationContext()
            .getBeansOfType(StopPlaceChangelogListener.class)
            .values();
        
        listeners.forEach(stopPlaceChangelog::registerStopPlaceChangelogListener);
    }
}
```

That's it! The starter will automatically configure all necessary components.

## Configuration Properties

### Basic Configuration

```properties
# Enable the stopplace changelog (default: false)
org.rutebanken.helper.stopplace.changelog.enabled=true

# Kafka broker configuration
org.rutebanken.helper.stopplace.changelog.kafka.bootstrap-servers=localhost:9092
org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-url=http://localhost:8081
org.rutebanken.helper.stopplace.changelog.kafka.topic=stopplace-changelog

# Stop place repository URL for fetching stop place data
org.rutebanken.helper.stopplace.changelog.repository.url=https://api.entur.io/stop-places/v1
```

### SASL Authentication (for production environments)

```properties
org.rutebanken.helper.stopplace.changelog.kafka.security-protocol=SASL_SSL
org.rutebanken.helper.stopplace.changelog.kafka.sasl-mechanism=SCRAM-SHA-512
org.rutebanken.helper.stopplace.changelog.kafka.sasl-jaas-config=org.apache.kafka.common.security.scram.ScramLoginModule required username="your-username" password="your-password";

# Schema Registry authentication
org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-basic-auth-user-info=username:password
```

### Optional Configuration

```properties
# Whether to automatically start the listener on application startup (default: true)
org.rutebanken.helper.stopplace.changelog.kafka.autostartup=true

# Consumer group (if not set, each instance gets a unique group ID)
org.rutebanken.helper.stopplace.changelog.kafka.group-id=my-consumer-group

# Fetch all versions or just the latest version of StopPlace (default: true)
org.rutebanken.helper.stopplace.changelog.repository.allVersions=false

# Control which related entities are exported with the stop place data
# Possible values: ALL, RELEVANT, NONE (default: RELEVANT)

# Topographic places (municipalities, counties, etc.)
org.rutebanken.helper.stopplace.changelog.repository.topographicPlaceExportMode=RELEVANT

# Tariff zones
org.rutebanken.helper.stopplace.changelog.repository.tariffZoneExportMode=RELEVANT

# Groups of tariff zones
org.rutebanken.helper.stopplace.changelog.repository.groupOfTariffZonesExportMode=RELEVANT

# Fare zones
org.rutebanken.helper.stopplace.changelog.repository.fareZoneExportMode=RELEVANT

# Groups of stop places
org.rutebanken.helper.stopplace.changelog.repository.groupOfStopPlacesExportMode=RELEVANT
```

#### Export Mode Values

- **ALL**: Export all entities of this type
- **RELEVANT**: Export only entities referenced by the stop place (default)
- **NONE**: Do not export entities of this type

## Auto-Configuration

The starter provides the following auto-configured beans:

- `StopPlaceChangelog`: Main interface for registering listeners
- `StopPlaceRepository`: HTTP client for fetching stop place data
- `ChangelogConsumerController`: Controller for starting/stopping the Kafka consumer
- `KafkaListenerContainerFactory`: Properly configured Kafka listener factory
- `PublicationTimeRecordFilterStrategy`: Filters old events based on startup time

### Conditional Configuration

The auto-configuration is activated when:

- `org.rutebanken.helper.stopplace.changelog.enabled=true`
- Required properties are set (bootstrap-servers, schema-registry-url, topic, repository.url)
- Spring Kafka is on the classpath

## Event Types

- **CREATE**: A new stop place has been created
- **UPDATE**: An existing stop place has been updated  
- **REMOVE**: A stop place has been deactivated (soft delete)
- **DELETE**: A stop place has been permanently deleted

## Event Schema

The library consumes events with the following Avro schema:

```json
{
    "type": "record",
    "name": "StopPlaceChangelogEvent",
    "namespace": "org.rutebanken.irkalla.avro",
    "fields": [
        {
            "name": "stopPlaceId",
            "type": "string",
            "doc": "StopPlace netex id"
        },
        {
            "name": "stopPlaceVersion",
            "type": "long",
            "doc": "StopPlace version"
        },
        {
            "name": "stopPlaceChanged",
            "type": ["null", {"type": "long", "logicalType": "timestamp-millis"}],
            "default": null,
            "doc": "Epoch timestamp when the change happened"
        },
        {
            "name": "eventType",
            "type": {
                "name": "EnumType",
                "type": "enum",
                "symbols": ["CREATE", "UPDATE", "REMOVE", "DELETE"]
            },
            "doc": "Stop place change types"
        }
    ]
}
```

## Advanced Configuration

### Custom WebClient Configuration

The library uses Spring's WebClient for HTTP operations with sensible defaults (60s timeout, 10MB buffer). You can provide your own WebClient bean to customize these settings:

```java
@Configuration
public class MyWebClientConfig {
    
    @Bean("tiamatWebClient")
    public WebClient customWebClient(WebClient.Builder builder) {
        return builder
            .defaultHeader("User-Agent", "MyApp/1.0")
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(20 * 1024 * 1024) // 20MB buffer
            )
            .build();
    }
}
```

### Custom Publication Time Filter

You can provide a custom `PublicationTimeRecordFilterStrategy` bean to filter events based on your specific requirements:

```java
@Configuration
public class MyChangelogConfig {
    
    @Bean("publicationTimeRecordFilterStrategy")
    public PublicationTimeRecordFilterStrategy customFilter() {
        // Only process events after a specific time
        Instant cutoffTime = Instant.parse("2023-01-01T00:00:00Z");
        return new PublicationTimeRecordFilterStrategy(cutoffTime);
    }
}
```

### Multiple Consumer Instances

By default, each application instance gets a unique consumer group ID, meaning every instance will receive all messages. This is useful when each instance needs to process all events independently.

If you want load balancing across instances, set a common group ID:

```properties
org.rutebanken.helper.stopplace.changelog.kafka.group-id=shared-consumer-group
```

## Programmatic Control

You can control the Kafka consumer programmatically using the `ChangelogConsumerController`:

```java
@Service
public class MyService {
    
    @Autowired
    private ChangelogConsumerController consumerController;
    
    public void pauseConsumer() {
        consumerController.stop();
    }
    
    public void resumeConsumer() {
        consumerController.start();
    }
}
```
