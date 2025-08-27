# StopPlace Changelog

A Spring Boot library for consuming stop place change events from Kafka. This library provides a simple way to listen to stop place lifecycle events (create, update, deactivate, delete) and integrate them into your application.

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>stopplace-changelog</artifactId>
    <version>5.41.0-SNAPSHOT</version>
</dependency>
```

## Configuration

### Basic Configuration

```properties
# Enable the stopplace changelog
org.rutebanken.helper.stopplace.changelog.kafka=true

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

# Whether to automatically start the listener on application startup.
# If false, the listener can be started programmatically through the KafkaListenerEndpointRegistry.
# The listener id is 'tiamatChangelogListener'
org.rutebanken.helper.stopplace.changelog.kafka.autostartup=true

# Consumer group (if not set, each instance gets a unique group ID)
org.rutebanken.helper.stopplace.changelog.kafka.group-id=my-consumer-group

# Fetch all versions or just the latest version of StopPlace (default: true)
org.rutebanken.helper.stopplace.changelog.repository.allVersions=false

# Control which related entities are exported with the stop place data
# Possible values for all export modes: ALL, RELEVANT, NONE (default: RELEVANT)

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

- **ALL**: Export all related entities of this type
- **RELEVANT**: Export only entities directly related to the stop place (default)
- **NONE**: Do not export entities of this type

## Usage

### 1. Enable Component Scanning

Make sure your Spring Boot application scans the stopplace-changelog package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"your.package", "org.rutebanken.helper.stopplace.changelog"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 2. Implement StopPlaceChangelogListener

Create a service that implements the `StopPlaceChangelogListener` interface:

```java
@Service
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

Inject the `StopPlaceChangelog` and register your listener:

```java
@Service
public class StopPlaceService {
    
    private final StopPlaceChangelog stopPlaceChangelog;
    private final MyStopPlaceHandler handler;
    
    public StopPlaceService(StopPlaceChangelog stopPlaceChangelog, 
                           MyStopPlaceHandler handler) {
        this.stopPlaceChangelog = stopPlaceChangelog;
        this.handler = handler;
    }
    
    @PostConstruct
    public void init() {
        stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
    }
    
    @PreDestroy
    public void cleanup() {
        stopPlaceChangelog.unregisterStopPlaceChangelogListener(handler);
    }
}
```

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

## Event Types

- **CREATE**: A new stop place has been created
- **UPDATE**: An existing stop place has been updated
- **REMOVE**: A stop place has been deactivated (soft delete)
- **DELETE**: A stop place has been permanently deleted

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

## Development

### Building

```bash
mvn clean compile
```

### Running Tests

```bash
mvn test
```

### Generating Avro Classes

Avro classes are automatically generated from the schema in `src/main/avro/` during the compile phase.
