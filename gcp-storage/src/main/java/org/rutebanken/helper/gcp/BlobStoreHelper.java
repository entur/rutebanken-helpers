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
import com.google.cloud.ReadChannel;
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
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class BlobStoreHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreHelper.class);

    private static final int BUFFER_CHUNK_SIZE_RETRY_UPLOAD = 1024 * 1024;
    private static final List<Integer> RETRY_BACKOFF_SECONDS = Arrays.asList(0, 5, 30);

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

    public static Blob uploadBlobWithRetry(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic, String contentType) {
        LOGGER.debug("Uploading blob {} to bucket {}", name, containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();

        byte[] buffer = new byte[BUFFER_CHUNK_SIZE_RETRY_UPLOAD];

        try (WriteChannel orgChannel = storage.writer(blobInfo)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                writeWithRetry(blobId, ByteBuffer.wrap(buffer, 0, bytesRead), orgChannel);
            }
        } catch (IOException ioE) {
            throw new BlobStoreException("Blob upload of blob with name '" + blobId.getName() + "' failed: " + ioE.getMessage(), ioE);
        }
        Blob blob = storage.get(blobId);
        LOGGER.debug("Stored blob with name '{}' and size '{}' in bucket '{}'", blob.getName(), blob.getSize(), blob.getBucket());
        return blob;
    }

    private static void writeWithRetry(BlobId blobId, ByteBuffer buffer, WriteChannel writer) {
        Iterator<Integer> backOffSecItr = RETRY_BACKOFF_SECONDS.iterator();
        boolean doTry = true;
        writer.setChunkSize(buffer.limit());
        while (doTry) {
            try {
                writer.write(buffer);
                doTry = false;
            } catch (Exception e) {
                if (backOffSecItr.hasNext()) {
                    Integer backOffSec = backOffSecItr.next();
                    LOGGER.info("Blob upload of blob with name '{}' failed  for chunk. Attempting retry in {} seconds. Error: {}", blobId.getName(), backOffSec, e.getMessage());
                    try {
                        Thread.sleep(backOffSec * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BlobStoreException("Interrupted while waiting to retry upload of blob with name '" + blobId.getName() + "' ", ie);
                    }
                } else {
                    throw new BlobStoreException("Blob upload of blob with name '" + blobId.getName() + "' failed. No more retries available. Error: " + e.getMessage());
                }

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
