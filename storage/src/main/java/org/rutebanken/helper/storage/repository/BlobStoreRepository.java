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

package org.rutebanken.helper.storage.repository;

import jakarta.annotation.Nullable;
import org.rutebanken.helper.storage.BlobAlreadyExistsException;
import org.rutebanken.helper.storage.model.BlobDescriptor;

import java.io.InputStream;

/**
 * Repository for managing binary files.
 * See GcsBlobStoreRepository in the library org.entur.ror.helpers:gcp-storage for an implementation that uses
 * Google Cloud Storage as the storage backend.
 * A simple implementation {@link LocalDiskBlobStoreRepository} uses local disk as the storage backend (for testing purpose).
 * A simple implementation {@link InMemoryBlobStoreRepository} uses an in-memory map as the storage backend (for testing purpose).
 */
public interface BlobStoreRepository {


    /**
     * Return true if the given blob exists in the repository.
     * The default implementation retrieves the object and test for nullity.
     * Specific implementations can provide an optimized algorithm.
     */
    default boolean exist(String objectName) {
      return getBlob(objectName) != null;
    }

    /**
     * Download a blob from storage.
     *
     * @param objectName the name of the blob
     * @return an InputStream on the file content or null if the object does not exist.
     */
    @Nullable
    InputStream getBlob(String objectName);

    /**
     * Upload a blob and return its generation number.
     *
     * @param blobDescriptor Container type describing the blob to upload.
     * @return the blob generation
     * @see #uploadBlob(String, InputStream)
     */
    default long uploadBlob(BlobDescriptor blobDescriptor) {
        if (blobDescriptor.contentType().isPresent()) {
            return uploadBlob(blobDescriptor.name(), blobDescriptor.inputStream(), blobDescriptor.contentType().get());
        } else {
            return uploadBlob(blobDescriptor.name(), blobDescriptor.inputStream());
        }
    }

    /**
     * Upload a blob and returns its generation number.
     *
     * @param objectName  the name of the blob
     * @param inputStream the blob data
     * @return the blob generation
     */
    long uploadBlob(String objectName, InputStream inputStream);

    /**
     * Upload a blob and returns its generation number.
     *
     * @param objectName  the name of the blob
     * @param inputStream the blob data
     * @param contentType the blob content type
     * @return the blob generation
     */
    long uploadBlob(String objectName, InputStream inputStream, String contentType);

    /**
     * Upload a blob and returns its generation number. Fails if the blob already exists.
     *
     * @param objectName  the name of the blob.
     * @param inputStream the blob data.
     * @return the blob generation.
     * @throws BlobAlreadyExistsException if the blob already exists.
     */
    long uploadNewBlob(String objectName, InputStream inputStream);

    /**
     * Copy a blob to another container.
     */
    void copyBlob(String sourceContainerName, String sourceObjectName, String targetContainerName, String targetObjectName);

    /**
     * Copy a blob to another container. The specified version is copied.
     */
    void copyVersionedBlob(String sourceContainerName, String sourceObjectName, Long sourceVersion, String targetContainerName, String targetObjectName);

    /**
     * Copy all blobs under a specific prefix (folder) to another container.
     */
    void copyAllBlobs(String sourceContainerName, String prefix, String targetContainerName, String targetPrefix);

    /**
     * Delete a blob.
     *
     * @return true if the blob was deleted.
     */
    boolean delete(String objectName);

    /**
     * Delete all blobs under a specific prefix (folder)
     *
     * @return true if at least one blob was deleted.
     */
    boolean deleteAllFilesInFolder(String folder);

    /**
     * Specify the name of the container.
     */
    void setContainerName(String containerName);


}
