# storage-gcp-gcs

Google Cloud Storage (GCS) implementation of the [`BlobStoreRepository`](../storage/README.md) interface. Supports full numeric versioning (GCS object generations).

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>storage-gcp-gcs</artifactId>
    <version>VERSION</version>
</dependency>
```

## Key Classes

### `GcsBlobStoreRepository`

Implements `BlobStoreRepository` backed by Google Cloud Storage.

**Constructors:**

```java
// With explicit credentials file
new GcsBlobStoreRepository("my-gcp-project", "/path/to/service-account.json");

// With Application Default Credentials (recommended for GKE/Cloud Run)
new GcsBlobStoreRepository("my-gcp-project", null);

// With pre-configured Storage client
new GcsBlobStoreRepository(storageClient);
```

### `BlobStoreHelper`

Static utility class wrapping common GCS operations. Useful when you need lower-level control.

| Method | Description |
|--------|-------------|
| `existBlob(storage, container, name)` | Check if a blob exists |
| `getBlob(storage, container, name)` | Download as `InputStream` |
| `createOrReplace(storage, container, descriptor)` | Upload, overwriting if present |
| `createNew(storage, container, descriptor)` | Upload, failing if already present |
| `listAllBlobsRecursively(storage, container, prefix)` | List blobs under a prefix |
| `delete(storage, container, name)` | Delete a blob |
| `deleteBlobsByPrefix(storage, container, prefix)` | Delete all blobs under a prefix |

## Usage

```java
GcsBlobStoreRepository repository = new GcsBlobStoreRepository("my-project", null);
repository.setContainerName("my-gcs-bucket");

// Upload
repository.uploadBlob("netex/export.xml", inputStream, "application/xml");

// Download
InputStream data = repository.getBlob("netex/export.xml");

// Copy a specific version
repository.copyVersionedBlob("source-bucket", "file.xml", 42L, "dest-bucket", "file.xml");

// Delete folder
repository.deleteAllFilesInFolder("netex/");
```

## Authentication

Uses [Google Application Default Credentials](https://cloud.google.com/docs/authentication/application-default-credentials) when no credentials file is specified. In GKE/Cloud Run, this resolves to the workload identity automatically.

## Testing

Integration tests use the GCloud TestContainers emulator — no real GCP project required.

---

[Back to root](../README.md)