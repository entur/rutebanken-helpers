/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package org.rutebanken.helper.gcp.repository;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import org.rutebanken.helper.gcp.BlobStoreHelper;
import org.rutebanken.helper.storage.model.BlobDescriptor;
import org.rutebanken.helper.storage.repository.BlobStoreRepository;

/**
 * Blob store repository targeting Google Cloud Storage.
 */

public class GcsBlobStoreRepository implements BlobStoreRepository {

  private final Storage storage;

  private String containerName;

  public GcsBlobStoreRepository(String projectId, String credentialPath) {
    this(buildStorage(projectId, credentialPath));
  }

  public GcsBlobStoreRepository(Storage storage) {
    this.storage = storage;
  }

  protected String containerName() {
    return containerName;
  }

  public void setContainerName(String containerName) {
    this.containerName = containerName;
  }

  @Override
  public boolean exist(String objectName) {
    return BlobStoreHelper.existBlob(storage, containerName, objectName);
  }

  @Override
  public InputStream getBlob(String name) {
    return BlobStoreHelper.getBlob(storage, containerName, name);
  }

  @Override
  public long uploadBlob(BlobDescriptor blobDescriptor) {
    Blob blob = BlobStoreHelper.createOrReplace(
      storage,
      containerName,
      blobDescriptor.name(),
      blobDescriptor.inputStream(),
      false,
      blobDescriptor.contentType().orElse(BlobStoreHelper.DEFAULT_CONTENT_TYPE),
      blobDescriptor.metadata().orElse(Map.of())
    );
    return blob.getGeneration();
  }

  @Override
  public long uploadBlob(String name, InputStream inputStream) {
    Blob blob = BlobStoreHelper.createOrReplace(
      storage,
      containerName,
      name,
      inputStream,
      false
    );
    return blob.getGeneration();
  }

  @Override
  public long uploadBlob(
    String name,
    InputStream inputStream,
    String contentType
  ) {
    Blob blob = BlobStoreHelper.createOrReplace(
      storage,
      containerName,
      name,
      inputStream,
      false,
      contentType
    );
    return blob.getGeneration();
  }

  @Override
  public long uploadNewBlob(String objectName, InputStream inputStream) {
    Blob blob = BlobStoreHelper.createNew(
      storage,
      containerName,
      objectName,
      inputStream,
      false
    );
    return blob.getGeneration();
  }

  @Override
  public void copyBlob(
    String sourceContainerName,
    String sourceObjectName,
    String targetContainerName,
    String targetObjectName
  ) {
    copyVersionedBlob(
      sourceContainerName,
      sourceObjectName,
      null,
      targetContainerName,
      targetObjectName
    );
  }

  @Override
  public void copyVersionedBlob(
    String sourceContainerName,
    String sourceObjectName,
    Long sourceVersion,
    String targetContainerName,
    String targetObjectName
  ) {
    Storage.CopyRequest request = Storage.CopyRequest
      .newBuilder()
      .setSource(
        BlobId.of(sourceContainerName, sourceObjectName, sourceVersion)
      )
      .setTarget(BlobId.of(targetContainerName, targetObjectName))
      .build();
    storage.copy(request).getResult();
  }

  @Override
  public void copyAllBlobs(
    String sourceContainerName,
    String prefix,
    String targetContainerName,
    String targetPrefix
  ) {
    Iterator<Blob> blobIterator = BlobStoreHelper.listAllBlobsRecursively(
      storage,
      sourceContainerName,
      prefix
    );
    while (blobIterator.hasNext()) {
      Blob blob = blobIterator.next();

      BlobInfo.Builder targetBlobInfoBuilder = BlobInfo.newBuilder(
        targetContainerName,
        blob.getName().replace(prefix, targetPrefix)
      );
      BlobId targetBlobId = targetBlobInfoBuilder.build().getBlobId();

      Storage.CopyRequest request = Storage.CopyRequest
        .newBuilder()
        .setSource(blob.getBlobId())
        .setTarget(targetBlobId)
        .build();
      Blob targetBlob = storage.copy(request).getResult();

      if (targetBlob.getName().endsWith(".html")) {
        BlobInfo updatedInfo = targetBlob
          .toBuilder()
          .setContentType("text/html")
          .build();
        storage.update(updatedInfo);
      }
    }
  }

  @Override
  public boolean delete(String objectName) {
    return BlobStoreHelper.delete(
      storage,
      BlobId.of(containerName, objectName)
    );
  }

  @Override
  public boolean deleteAllFilesInFolder(String folder) {
    return BlobStoreHelper.deleteBlobsByPrefix(storage, containerName, folder);
  }

  public Storage storage() {
    return storage;
  }

  private static Storage buildStorage(String projectId, String credentialPath) {
    if (credentialPath == null || credentialPath.isEmpty()) {
      // Use Default gcp credentials
      return BlobStoreHelper.getStorage(projectId);
    } else {
      return BlobStoreHelper.getStorage(credentialPath, projectId);
    }
  }
}
