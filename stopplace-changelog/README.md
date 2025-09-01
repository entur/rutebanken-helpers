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
