# storage

Core blob storage abstraction. Defines the `BlobStoreRepository` interface and supporting types used by all storage backend implementations.

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>storage</artifactId>
    <version>VERSION</version>
</dependency>
```

## Key Interfaces and Classes

### `BlobStoreRepository`

The central interface for all blob storage operations.

| Method | Description |
|--------|-------------|
| `getBlob(objectName)` | Download a blob as `InputStream` |
| `uploadBlob(name, stream, contentType)` | Upload a blob, overwriting if it exists |
| `uploadBlob(name, stream, contentType, metadata)` | Upload with custom metadata |
| `uploadNewBlob(descriptor)` | Upload, failing if the blob already exists |
| `copyBlob(srcContainer, srcName, destContainer, destName)` | Copy a blob between containers |
| `copyVersionedBlob(srcContainer, srcName, version, destContainer, destName)` | Copy a specific version |
| `copyAllBlobs(srcContainer, destContainer)` | Copy all blobs between containers |
| `delete(objectName)` | Delete a single blob; returns `true` if deleted |
| `deleteAllFilesInFolder(folder)` | Delete all blobs under a folder prefix |
| `setContainerName(containerName)` | Set the active bucket/container name |

### `BlobDescriptor`

Carries all metadata needed to upload a blob.

| Field | Type | Description |
|-------|------|-------------|
| `name` | `String` | Blob object name / path |
| `inputStream` | `InputStream` | Content to upload |
| `contentType` | `String` | MIME type (e.g. `application/octet-stream`) |
| `metadata` | `Map<String, String>` | Optional custom metadata key-value pairs |

## Usage

```java
BlobStoreRepository repository = /* inject an implementation */;
repository.setContainerName("my-bucket");

// Upload
repository.uploadBlob("data/file.txt", inputStream, "text/plain");

// Download
InputStream data = repository.getBlob("data/file.txt");

// Copy
repository.copyBlob("source-bucket", "data/file.txt", "dest-bucket", "data/file.txt");

// Delete
boolean deleted = repository.delete("data/file.txt");
```

## Implementations

| Module | Backend |
|--------|---------|
| [storage-aws-s3](../storage-aws-s3/README.md) | Amazon S3 |
| [storage-gcp-gcs](../storage-gcp-gcs/README.md) | Google Cloud Storage |

---

[Back to root](../README.md)