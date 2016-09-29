package org.rutebanken.helper.gcp;

import com.google.cloud.AuthCredentials;
import com.google.cloud.Page;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;

public class BlobStoreHelper {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Storage storage;

    public BlobStoreHelper(Storage storage) {
        this.storage = storage;
    }

    public Iterator<Blob> listAllBlobsRecursively(String prefix, String containerName){
        logger.debug("Listing blobs in bucket " + containerName + " with prefix " + prefix + " recursively.");
        Page<Blob> blobs = storage.list(containerName, Storage.BlobListOption.prefix(prefix));
        return blobs.iterateAll();
    }

    public Blob uploadBlob(String containerName, String name, InputStream inputStream, boolean makePublic) {
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

    public InputStream getBlob(String containerName, String name) {
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
