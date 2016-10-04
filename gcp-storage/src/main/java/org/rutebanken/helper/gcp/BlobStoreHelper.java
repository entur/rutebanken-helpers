package org.rutebanken.helper.gcp;

import com.google.cloud.AuthCredentials;
import com.google.cloud.Page;
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
import java.util.Iterator;

public class BlobStoreHelper {

    private static Logger logger = LoggerFactory.getLogger(BlobStoreHelper.class);

    private static final long FILE_SIZE_LIMIT = 1_000_000;
    private static final int BUFFER_CHUNK_SIZE = 1024;

    private BlobStoreHelper(){

    }

    public static Iterator<Blob> listAllBlobsRecursively(Storage storage, String containerName, String prefix){
        logger.debug("Listing blobs in bucket " + containerName + " with prefix " + prefix + " recursively.");
        Page<Blob> blobs = storage.list(containerName, Storage.BlobListOption.prefix(prefix));
        return blobs.iterateAll();
    }

    public static Blob uploadBlob(Storage storage, String containerName, String name, InputStream inputStream, boolean makePublic) {
        logger.debug("Uploading blob " + name + " to bucket " + containerName);
        BlobId blobId = BlobId.of(containerName, name);
        BlobInfo.Builder builder = BlobInfo.builder(blobId).contentType("application/octet-stream");
        if (makePublic) {
            builder.acl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
        }
        BlobInfo blobInfo = builder.build();
        Blob blob = storage.create(blobInfo, inputStream);
        logger.debug("Stored blob with name '" + blob.name() + "' and size '" + blob.size() + "' in bucket '" + blob.bucket() + "'");
        return blob;
    }

    public static void uploadBlob(Storage storage, String containerName, String blobPath, Path filePath, boolean makePublic) throws Exception {
        logger.debug("Uploading blob " + filePath.getFileName().toString() + " to bucket " + containerName);
        String blobIdName = blobPath + filePath.getFileName().toString();
        BlobId blobId = BlobId.of(containerName, blobIdName);
        BlobInfo.Builder builder = BlobInfo.builder(blobId).contentType("application/octet-stream");
        if (makePublic) {
            builder.acl(ImmutableList.of(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)));
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
        logger.debug("Stored blob with name '" + blobInfo.name() + "' and size '" + blobInfo.size() + "' in bucket '" + blobInfo.bucket() + "'");
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
                logger.debug("Retrieved blob with name '" + blob.name() + "' and size '" + blob.size() + "' from bucket '" + blob.bucket() + "'");
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        } else {
            logger.warn("Could not find '" + blobId.name() + "' in bucket '" + blobId.bucket() + "'");
        }
        return result;
    }

    public static boolean delete(Storage storage, BlobId blobId) {
        return storage.delete(blobId);
    }

    public static Storage getStorage(String credentialPath, String projectId) {
        try {
            StorageOptions options = StorageOptions.builder()
                    .projectId(projectId)
                    .authCredentials(AuthCredentials.createForJson(
                            new FileInputStream(credentialPath))).build();
            return options.service();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
