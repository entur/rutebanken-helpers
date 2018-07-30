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

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.api.gax.paging.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.*;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlobStoreHelper {

    private static Logger logger = LoggerFactory.getLogger(BlobStoreHelper.class);

    private static final long FILE_SIZE_LIMIT = 1_000_000;
    private static final int BUFFER_CHUNK_SIZE = 1024;

    private BlobStoreHelper(){

    }

    public static Iterator<Blob> listAllBlobsRecursively(Storage storage, String containerName, String prefix){
        logger.debug("Listing blobs in bucket " + containerName + " with prefix " + prefix + " recursively.");
        Page<Blob> blobs = storage.list(containerName, Storage.BlobListOption.prefix(prefix));
        return blobs.iterateAll().iterator();
    }

    /**
     * Deprecated as underlying API method is deprecated. Use byte[] content version.
     */
    @Deprecated
    public static Blob uploadBlob(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic) {
        return uploadBlob(storage, containerName, name, inputStream, makePublic, "application/octet-stream");
    }

    /**
     * Deprecated as underlying API method is deprecated. Use byte[] content version.
     */
    @Deprecated
    public static Blob uploadBlob(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic, String contentType) {
        logger.debug("Uploading blob " + name + " to bucket " + containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();
        Blob blob = storage.create(blobInfo, inputStream);
        logger.debug("Stored blob with name '" + blob.getName() + "' and size '" + blob.getSize() + "' in bucket '" + blob.getBucket() + "'");
        return blob;
    }

    public static Blob uploadBlob(Storage storage, String containerName, String name, byte[] content, boolean makePublic) {
        return uploadBlob(storage, containerName, name, content, makePublic, "application/octet-stream");
    }

    public static Blob uploadBlob(Storage storage, String containerName, String name, byte[] content, boolean makePublic, String contentType) {
        logger.debug("Uploading blob " + name + " to bucket " + containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType(contentType);
        if (makePublic) {
            builder.setAcl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();
        Blob blob = storage.create(blobInfo, content);
        logger.debug("Stored blob with name '" + blob.getName() + "' and size '" + blob.getSize() + "' in bucket '" + blob.getBucket() + "'");
        return blob;
    }

    @Deprecated
    public static void uploadBlob(Storage storage, String containerName, String blobPath, Path filePath, boolean makePublic) throws Exception {
        logger.debug("Uploading blob " + filePath.getFileName().toString() + " to bucket " + containerName);
        String blobIdName = blobPath + filePath.getFileName().toString();
        BlobId blobId = BlobId.of(containerName, blobIdName);
        BlobInfo.Builder builder = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream");
        if (makePublic) {
            builder.setAcl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();
        if (Files.size(filePath) > FILE_SIZE_LIMIT) {
            try (WriteChannel writer = storage.writer(blobInfo)) {
                byte[] buffer = new byte[BUFFER_CHUNK_SIZE];
                try (InputStream inputStream = Files.newInputStream(filePath)) {
                    int limit;
                    while ((limit = inputStream.read(buffer)) >= 0) {
                        writer.write(ByteBuffer.wrap(buffer, 0, limit));
                    }
                }
            }
        } else {
            byte[] bytes = Files.readAllBytes(filePath);
            storage.create(blobInfo, bytes);
        }
        logger.debug("Stored blob with name '" + blobInfo.getName() + "' and size '" + blobInfo.getSize() + "' in bucket '" + blobInfo.getBucket() + "'");
    }

    public static InputStream getBlob(Storage storage, String containerName, String name) {
        logger.debug("Fetching blob " + name + " from bucket " + containerName);
        BlobId blobId = BlobId.of(containerName, name);
        Blob blob = storage.get(blobId);
        InputStream result = null;
        if (blob != null) {
            try (ReadChannel reader = blob.reader()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                WritableByteChannel channel = Channels.newChannel(outputStream);
                ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
                while (reader.read(bytes) > 0) {
                    bytes.flip();
                    channel.write(bytes);
                    bytes.clear();
                }
                result = new ByteArrayInputStream(outputStream.toByteArray());
                logger.debug("Retrieved blob with name '" + blob.getName() + "' and size '" + blob.getSize() + "' from bucket '" + blob.getBucket() + "'");
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        } else {
            logger.warn("Could not find '" + blobId.getName() + "' in bucket '" + blobId.getBucket() + "'");
        }
        return result;
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
            throw new RuntimeException(e);
        }
    }

}
