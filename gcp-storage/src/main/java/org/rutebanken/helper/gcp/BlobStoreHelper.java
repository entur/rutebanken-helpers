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

package org.rutebanken.helper.gcp;

import com.google.api.client.http.HttpStatusCodes;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlobStoreHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreHelper.class);

    private static final int DEFAULT_CHUNK_SIZE = 15 * 1024 * 1024;
    private static final int CONNECT_AND_READ_TIMEOUT = 60000;
    private static final String DEFAULT_CACHE_CONTROL = "public, max-age=600";

    private BlobStoreHelper() {
    }

    public static Iterator<Blob> listAllBlobsRecursively(Storage storage, String containerName, String prefix) {
        LOGGER.debug("Listing blobs in bucket {} with prefix {} recursively.", containerName, prefix);
        Page<Blob> blobs = storage.list(containerName, Storage.BlobListOption.prefix(prefix));
        return blobs.iterateAll().iterator();
    }

    /**
     *
     * @deprecated Use {@link #createNew(Storage, String, String, InputStream, boolean)} or {@link #createOrReplace(Storage, String, String, InputStream, boolean)}
     */
    @Deprecated
    public static Blob uploadBlob(Storage storage, String containerName, String name, byte[] content, boolean makePublic) {
        return uploadBlob(storage, containerName, name, content, makePublic, "application/octet-stream");
    }

    /**
     * @deprecated Use {@link #createNew(Storage, String, String, InputStream, boolean, String)} or {@link #createOrReplace(Storage, String, String, InputStream, boolean, String)}
     */
    @Deprecated
    public static Blob uploadBlob(Storage storage, String containerName, String name, byte[] content, boolean makePublic, String contentType) {
        LOGGER.debug("Uploading blob {} to bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();
        Blob blob = storage.create(blobInfo, content);
        LOGGER.debug("Stored blob with name '{}' and size '{}' in bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
        return blob;
    }

    /**
     * @deprecated Use {@link #createNew(Storage, String, String, InputStream, boolean)} or {@link #createOrReplace(Storage, String, String, InputStream, boolean)}
     */
    @Deprecated
    public static Blob uploadBlobWithRetry(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic) {
        return uploadBlobWithRetry(storage, containerName, name, inputStream, makePublic, "application/octet-stream");
    }


    /**
     * Upload a blob using  a {@link Storage#writer(BlobInfo, Storage.BlobWriteOption...)} as it is recommended for bigger files.
     * Retry/resumable logic is handled internally by the GCS client library.
     *
     * @deprecated Use {@link #createNew(Storage, String, String, InputStream, boolean, String)} or {@link #createOrReplace(Storage, String, String, InputStream, boolean, String)}
     */
    @Deprecated
    public static Blob uploadBlobWithRetry(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic, String contentType) {
        LOGGER.debug("Uploading with retry blob {} to bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();
        try {
            writeWithRetry(storage, blobInfo, inputStream);
        } catch (IOException ioE) {
            throw new BlobStoreException("Blob upload of blob with name '" + blobId.getName() + "' failed: " + ioE.getMessage(), ioE);
        }
        Blob blob = storage.get(blobId);
        LOGGER.debug("Stored blob with name '{}' and size '{}' in bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
        return blob;
    }

    /**
     * Write the blob in chunks. Chunks are  retried internally by the GCS client library.
     */
    private static void writeWithRetry(Storage storage, BlobInfo blobInfo, InputStream inputStream) throws IOException {
        try (WriteChannel writer = storage.writer(blobInfo)) {
            byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) >= 0) {
                writer.write(ByteBuffer.wrap(buffer, 0, length));
            }
        }
    }


    /**
     * Creates a new blob in GCP. Fails if the blob already exists.
     * Use {@link #createOrReplace(Storage, String, String, InputStream, boolean)} if the blob may already be present in
     * the bucket.
     *
     * @return a reference to the newly created blob.
     * @throws BlobAlreadyExistsException if the blob already exists.
     * @throws BlobStoreException         if the blob creation fails.
     */
    public static Blob createNew(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic) {
        return createNew(storage, containerName, name, inputStream, makePublic, "application/octet-stream");
    }

    /**
     * Creates a new blob in GCP. Fails if the blob already exists.
     * Use {@link #createOrReplace(Storage, String, String, InputStream, boolean, String contentType)} if the blob may
     * already be present in the bucket.
     * Note: The client library will automatically retry in case of transient network or server-side error. See <a href="https://cloud.google.com/storage/docs/retry-strategy">...</a>
     * Note: The client library will automatically send data in retryable 15MB chunks.
     *
     * @return a reference to the newly created blob.
     * @throws BlobAlreadyExistsException if the blob already exists.
     * @throws BlobStoreException         if the blob creation fails.
     */
    public static Blob createNew(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic, String contentType) {
        LOGGER.debug("Creating new blob {} in bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
            builder.setCacheControl(DEFAULT_CACHE_CONTROL);
        }
        BlobInfo blobInfo = builder.build();
        try {
            Blob blob = storage.createFrom(blobInfo, inputStream, Storage.BlobWriteOption.doesNotExist());
            LOGGER.debug("Stored blob with name '{}' and size '{}' in bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
            return blob;
        } catch (StorageException e) {
            if (e.getCode() == HttpStatusCodes.STATUS_CODE_PRECONDITION_FAILED) {
                throw new BlobAlreadyExistsException("The blob with name '" + blobId.getName() + "' already exists", e);
            } else {
                throw new BlobStoreException("Blob upload of blob with name '" + blobId.getName() + "' failed: " + e.getMessage(), e);
            }
        } catch (IOException ioE) {
            throw new BlobStoreException("Blob upload of blob with name '" + blobId.getName() + "' failed: " + ioE.getMessage(), ioE);
        }
    }

    /**
     * Creates or replace a blob in GCP. Fails if the blob is modified concurrently.
     * Use {@link #createNew(Storage, String, String, InputStream, boolean)} instead if there is a guarantee that the
     * blob does not exist in the bucket since createNew() requires one less return trip to the server.
     *
     * @return a reference to the created or modified blob.
     * @throws BlobConcurrentUpdateException if the blob is modified concurrently by another client.
     * @throws BlobStoreException            if the blob creation fails.
     */
    public static Blob createOrReplace(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic) {
        return createOrReplace(storage, containerName, name, inputStream, makePublic, "application/octet-stream");
    }

    /**
     * Creates or replace a blob in GCP. Fails if the blob is modified concurrently.
     * Use {@link #createNew(Storage, String, String, InputStream, boolean, String contentType)} instead if there is a guarantee that the
     * blob does not exist in the bucket since createNew() requires one less return trip to the server.
     * Note: The client library will automatically retry in case of transient network or server-side error. See <a href="https://cloud.google.com/storage/docs/retry-strategy">...</a>
     * Note: The client library will automatically send data in retryable 15MB chunks.
     *
     * @return a reference to the created or modified blob.
     * @throws BlobConcurrentUpdateException if the blob is modified concurrently by another client.
     * @throws BlobStoreException            if the blob creation fails.
     */
    public static Blob createOrReplace(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic, String contentType) {
        LOGGER.debug("Creating or replace blob {} in bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        Blob existingBlob = storage.get(blobId);
        if (existingBlob == null) {
            return createNew(storage, containerName, name, inputStream, makePublic, contentType);
        } else {
            BlobInfo.Builder builder = BlobInfo.newBuilder(existingBlob.getBlobId()).setContentType(contentType);
            if (makePublic) {
                builder.setAcl(List.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
                builder.setCacheControl(DEFAULT_CACHE_CONTROL);
            }
            BlobInfo blobInfo = builder.build();
            try {
                Blob blob = storage.createFrom(blobInfo, inputStream, Storage.BlobWriteOption.generationMatch());
                LOGGER.debug("Stored blob with name '{}' and size '{}' in bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
                return blob;
            } catch (StorageException e) {
                if (e.getCode() == HttpStatusCodes.STATUS_CODE_PRECONDITION_FAILED) {
                    throw new BlobConcurrentUpdateException("The blob with name '" + blobId.getName() + "' was updated concurrently", e);
                } else {
                    throw new BlobStoreException("Blob upload of blob with name '" + blobId.getName() + "' failed: " + e.getMessage(), e);
                }
            } catch (IOException ioE) {
                throw new BlobStoreException("Blob upload of blob with name '" + blobId.getName() + "' failed: " + ioE.getMessage(), ioE);
            }
        }
    }

    public static boolean existBlob(Storage storage, String containerName, String name) {
        LOGGER.debug("Checking that blob {} from bucket {} exists", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        Blob blob = storage.get(blobId);
        return blob != null;
    }

    public static InputStream getBlob(Storage storage, String containerName, String name) {
        LOGGER.debug("Fetching blob {} from bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        Blob blob = storage.get(blobId);
        if (blob != null) {
            LOGGER.debug("Retrieved blob with name '{}' and size '{}' from bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
            byte[] blobContent = blob.getContent();
            String serverMd5 = blob.getMd5ToHexString();
            String clientMd5 = DigestUtils.md5Hex(blobContent);
            if (!clientMd5.equals(serverMd5)) {
                throw new BlobStoreException("Client MD5 checksum (" + clientMd5 + ") and server MD5 checksum(" + serverMd5 + ") do not match");
            } else {
                return new ByteArrayInputStream(blobContent);
            }
        } else {
            LOGGER.info("File '{}' in bucket '{}' does not exist", blobId.getName(), blobId.getBucket());
            return null;
        }
    }

    public static boolean delete(Storage storage, BlobId blobId) {
        return storage.delete(blobId);
    }

    public static boolean deleteBlobsByPrefix(Storage storage, String containerName, String prefix) {
        List<BlobId> blobIdList = new ArrayList<>();
        listAllBlobsRecursively(storage, containerName, prefix).forEachRemaining(blob -> blobIdList.add(blob.getBlobId()));
        if (blobIdList.isEmpty()) {
            return false;
        }
        return storage.delete(blobIdList).stream().allMatch(ret -> ret);
    }

    private static StorageOptions.Builder getBuilder(String projectId) {
        // prevent copy operations from timing out when copying blobs across buckets
        // see https://github.com/googleapis/google-cloud-java/issues/2243
        HttpTransportOptions transportOptions = StorageOptions.getDefaultHttpTransportOptions();
        transportOptions = transportOptions.toBuilder().setConnectTimeout(CONNECT_AND_READ_TIMEOUT).setReadTimeout(CONNECT_AND_READ_TIMEOUT)
                .build();

        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setTransportOptions(transportOptions);
    }
    public static Storage getStorage(String credentialPath, String projectId) {
        try {
            return getBuilder(projectId)
                    .setCredentials(ServiceAccountCredentials.fromStream(Files.newInputStream(Paths.get(credentialPath))))
                    .build().getService();
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }
    public static Storage getStorage(String projectId) {
        return getBuilder(projectId)
                .build().getService();
    }

}
