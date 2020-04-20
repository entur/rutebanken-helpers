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

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlobStoreHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreHelper.class);

    private static final int DEFAULT_CHUNK_SIZE = 15 * 1024 * 1024;

    private BlobStoreHelper() {
    }

    public static Iterator<Blob> listAllBlobsRecursively(Storage storage, String containerName, String prefix) {
        LOGGER.debug("Listing blobs in bucket {} with prefix {} recursively.", containerName, prefix);
        Page<Blob> blobs = storage.list(containerName, Storage.BlobListOption.prefix(prefix));
        return blobs.iterateAll().iterator();
    }

    public static Blob uploadBlob(Storage storage, String containerName, String name, byte[] content, boolean makePublic) {
        return uploadBlob(storage, containerName, name, content, makePublic, "application/octet-stream");
    }

    public static Blob uploadBlob(Storage storage, String containerName, String name, byte[] content, boolean makePublic, String contentType) {
        LOGGER.debug("Uploading blob {} to bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();
        Blob blob = storage.create(blobInfo, content);
        LOGGER.debug("Stored blob with name '{}' and size '{}' in bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
        return blob;
    }

    public static Blob uploadBlobWithRetry(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic) {
        return uploadBlobWithRetry(storage, containerName, name, inputStream, makePublic, "application/octet-stream");
    }


    /**
     * Upload a blob using  a {@link Storage#writer(BlobInfo, Storage.BlobWriteOption...)} as it is recommended for bigger files.
     * Retry/resumable logic is handled internally by the GCS client library.
     * To be replaced with Blob.uploadFrom() when the method is available. See also  https://github.com/googleapis/java-storage/issues/40
     */
    public static Blob uploadBlobWithRetry(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic, String contentType) {
        LOGGER.debug("Uploading blob {} to bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
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

    public static InputStream getBlob(Storage storage, String containerName, String name) {
        LOGGER.debug("Fetching blob {} from bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        Blob blob = storage.get(blobId);
        if (blob != null) {
            LOGGER.debug("Retrieved blob with name '{}' and size '{}' from bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
            return new ByteArrayInputStream((blob.getContent()));
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

    public static Storage getStorage(String credentialPath, String projectId) {
        try {
            return StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(credentialPath)))
                    .setProjectId(projectId)
                    .build().getService();
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

}
