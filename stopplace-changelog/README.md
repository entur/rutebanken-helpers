# StopPlace Changelog

Core library for consuming stop place change events from Kafka. This library provides the fundamental interfaces and implementation for listening to stop place lifecycle events.

## Recommended Usage

**For most use cases, use the [stopplace-changelog-starter](../stopplace-changelog-starter/) instead of this core library directly.** The starter provides Spring Boot auto-configuration and simplified setup.

## Core Components

This library provides:

- `StopPlaceChangelog` interface for registering event listeners
- `StopPlaceChangelogListener` interface for handling events
- Kafka consumer implementation with Avro deserialization
- HTTP repository for fetching stop place data
- Publication time filtering for events

## Avro Class Allowlist

Since Avro 1.12.2, classes resolved by name during deserialization must be allowlisted in
`org.apache.avro.util.ClassSecurityValidator`. The Kafka consumer uses the Confluent
`KafkaAvroDeserializer` with `specific.avro.reader=true`, which resolves the generated
`StopPlaceChangelogEvent` class by name. The starter registers the generated classes
automatically. When using this core module without the starter, call
`StopPlaceChangelogAvroClassSecurity.trustGeneratedClasses()` once before the first record is
deserialized, otherwise every poll fails with
`SecurityException: Forbidden org.rutebanken.irkalla.avro.StopPlaceChangelogEvent!`.

## Event Types

- **CREATE**: A new stop place has been created
- **UPDATE**: An existing stop place has been updated
- **REMOVE**: A stop place has been deactivated (soft delete) 
- **DELETE**: A stop place has been permanently deleted

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

Avro classes are automatically generated from the schema in `target/generated-sources` during the compile phase.

---

[Back to root](../README.md)
