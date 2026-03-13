# storage-aws-s3

AWS S3 implementation of the [`BlobStoreRepository`](../storage/README.md) interface.

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>storage-aws-s3</artifactId>
    <version>VERSION</version>
</dependency>
```

## Key Class

### `S3BlobStoreRepository`

Implements `BlobStoreRepository` using the AWS SDK v2 S3 client.

**Notes:**
- S3 does not support numeric versioning — `copyVersionedBlob` always uses version `0` (S3 uses ETags internally).
- Custom metadata values containing non-ASCII characters are Base64-encoded automatically to comply with HTTP header constraints.

## Setup

Construct with a configured `S3Client` from the AWS SDK:

```java
S3Client s3Client = S3Client.builder()
    .region(Region.EU_WEST_1)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .build();

S3BlobStoreRepository repository = new S3BlobStoreRepository(s3Client);
repository.setContainerName("my-s3-bucket");
```

## Usage

```java
// Upload
repository.uploadBlob("imports/data.xml", inputStream, "application/xml");

// Download
InputStream data = repository.getBlob("imports/data.xml");

// Delete
repository.delete("imports/data.xml");
```

## Testing

Integration tests use [LocalStack](https://localstack.cloud/) via TestContainers to emulate S3 locally — no real AWS account required for tests.

---

[Back to root](../README.md)