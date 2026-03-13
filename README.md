# Rutebanken-helpers [![Build and push](https://github.com/entur/rutebanken-helpers/actions/workflows/push.yml/badge.svg)](https://github.com/entur/rutebanken-helpers/actions/workflows/push.yml) [![Maven Central](https://img.shields.io/maven-central/v/org.entur.ror.helpers/helper)](https://central.sonatype.com/artifact/org.entur.ror.helpers/helper) [![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL_v1.2-blue.svg)](https://joinup.ec.europa.eu/software/page/eupl) [![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://adoptium.net/)

A collection of shared Java libraries for reuse across Entur's route database, journey planning, and organizational management projects.

**License**: EUPL v1.2 | **Build**: Maven | **Java**: 17+

---

## Modules

| Module | Description |
|--------|-------------|
| [storage](storage/README.md) | Core blob storage abstraction interface |
| [storage-aws-s3](storage-aws-s3/README.md) | AWS S3 implementation of blob storage |
| [storage-gcp-gcs](storage-gcp-gcs/README.md) | Google Cloud Storage implementation of blob storage |
| [hazelcast4-helper](hazelcast4-helper/README.md) | Distributed caching via Hazelcast for Kubernetes |
| [calendar-helper](calendar-helper/README.md) | Calendar pattern analysis from date sets |
| [organisation](organisation/README.md) | Role-based authorization and JWT role extraction |
| [oauth2](oauth2/README.md) | OAuth2/JWT authentication with multi-issuer support |
| [permission-store-proxy](permission-store-proxy/README.md) | Baba API integration for user/role management |
| [slack](slack/README.md) | Slack webhook notification service |
| [entur-google-pubsub](entur-google-pubsub/README.md) | Google Cloud Pub/Sub base configuration |
| [stopplace-changelog](stopplace-changelog/README.md) | Kafka-based stop place event streaming |
| [stopplace-changelog-starter](stopplace-changelog-starter/README.md) | Spring Boot auto-configuration for stop place changelog |

---

## Dependency Graph

```
permission-store-proxy
  ├── organisation
  └── oauth2

oauth2
  └── organisation

stopplace-changelog-starter
  └── stopplace-changelog

storage-aws-s3
  └── storage (interface)

storage-gcp-gcs
  └── storage (interface)
```

---

## Adding a Dependency

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>MODULE_NAME</artifactId>
    <version>VERSION</version>
</dependency>
```

Replace `MODULE_NAME` with the desired module (e.g., `oauth2`, `organisation`, `storage-gcp-gcs`) and `VERSION` with the current release version.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) if present, or open a GitHub issue/PR on [entur/rutebanken-helpers](https://github.com/entur/rutebanken-helpers).