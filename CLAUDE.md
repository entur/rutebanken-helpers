# Rutebanken-helpers — Claude Guide

## Project Overview

Multi-module Maven library project maintained by Entur. Provides shared Java utilities for cloud storage, caching, OAuth2/JWT authentication, role-based authorization, Slack notifications, Google Pub/Sub, and Kafka-based stop place event streaming.

- **Group ID**: `org.entur.ror.helpers`
- **Root artifact**: `helper`
- **Java**: 17 (Liberica distribution in CI)
- **License**: EUPL v1.2

## Active Modules

| Module | Artifact ID | Purpose |
|--------|-------------|---------|
| `storage` | `storage` | `BlobStoreRepository` interface — core blob storage abstraction |
| `storage-aws-s3` | `storage-aws-s3` | AWS S3 implementation of blob storage |
| `storage-gcp-gcs` | `storage-gcp-gcs` | Google Cloud Storage implementation of blob storage |
| `hazelcast4-helper` | `hazelcast4-helper` | Hazelcast distributed cache setup for Kubernetes |
| `calendar-helper` | `calendar-helper` | Day-of-week pattern detection from date sets |
| `organisation` | `organisation` | Role-based authorization, JWT role extraction |
| `oauth2` | `oauth2` | OAuth2/JWT auth, multi-issuer support, authorized WebClient |
| `permission-store-proxy` | `permission-store-proxy` | Baba API integration for user/role management |
| `slack` | `slack` | Slack webhook notification service |
| `entur-google-pubsub` | `entur-google-pubsub` | Google Cloud Pub/Sub base configuration |
| `stopplace-changelog` | `stopplace-changelog` | Kafka consumer for stop place lifecycle events |
| `stopplace-changelog-starter` | `stopplace-changelog-starter` | Spring Boot auto-configuration for stop place changelog |

Directories without a `pom.xml` (e.g. `gcp-storage`, `hazelcast-helper`, `hubot`, `jms-batch`, `logging`) are **legacy/archived** — do not modify them.

## Build & Test

```bash
# Full build with tests and format check
mvn package

# Skip format check (faster local iteration)
mvn package -DskipPrettierCheck

# Fix formatting before committing
mvn prettier:write

# Build a single module
mvn package -pl storage-gcp-gcs -am
```

CI uses `mvn package -PprettierCheck`. PRs will fail if `prettier:write` has not been run.

## Code Formatting

Prettier for Java is enforced. Always run `mvn prettier:write` before committing. Do not manually reformat code in ways that conflict with Prettier output.

## Module Dependencies

```
permission-store-proxy → organisation, oauth2
oauth2                 → organisation
stopplace-changelog-starter → stopplace-changelog
storage-aws-s3         → storage
storage-gcp-gcs        → storage
```

## Key Conventions

- **Spring Boot auto-configuration**: `entur-google-pubsub` and `stopplace-changelog-starter` register beans via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- **Extension by subclassing**: `HazelCastService` is designed to be subclassed; override `updateDefaultMapConfig()`, `getAdditionalMapConfigurations()`, `getSerializerConfigs()`.
- **JWT role claims**: Role assignments use compact field names (`r`, `o`, `z`, `e`) to keep JWT tokens small. See `organisation/RoleAssignment.java`.
- **Blob storage**: All implementations share the `BlobStoreRepository` interface. Switch backends by swapping the implementation bean.
- **Caching**: `permission-store-proxy` uses Caffeine (10s TTL). `hazelcast4-helper` provides distributed Hazelcast cache.

## Testing

- Uses **JUnit 5** and **Hamcrest**.
- Integration tests for storage modules use **TestContainers** (LocalStack for S3, GCloud emulator for GCS, embedded Kafka for stopplace-changelog).
- For authorization testing, use `FullAccessAuthorizationService` to bypass permission checks.
- CI skips commits containing `ci skip` in the message.

## Release Process

- Merges to `master` trigger an automatic release to Maven Central (minor version bump).
- Do **not** manually update versions in `pom.xml` — the release workflow handles this.
- Exception: hotfix version bumps may be done manually with `mvn versions:set`.

## Module-level CLAUDE.md Files

Some modules have their own `CLAUDE.md` with deeper implementation notes:
- `oauth2/CLAUDE.md`
- `organisation/CLAUDE.md`
- `permission-store-proxy/CLAUDE.md`