package org.rutebanken.helper.gcp;


import com.google.cloud.AuthCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class BlobStoreHelperTest {

    private static final String BUCKET_NAME = "marduk-test";
    private static final String credentialPath = "/home/tomgag/.ssh/Carbon-a4d50ca8176c.json";
    private static final String directory = "test/";

    private static Storage storage;
    private static String projectId = "carbon-1287";
    private final String fileName = "avinor-netex.zip";
    private final String blobName = directory + fileName;

    @BeforeAll
    public static void beforeAll() throws IOException {
        StorageOptions options = StorageOptions.builder()
                .projectId(projectId)
                .authCredentials(AuthCredentials.createForJson(
                        new FileInputStream(credentialPath))).build();
        storage = options.service();
    }

    @BeforeEach
    public void beforeEach() {
        // Eating our own dog food here
        Iterator<Blob> blobIterator = BlobStoreHelper.listAllBlobsRecursively(storage, BUCKET_NAME, directory);
        blobIterator.forEachRemaining(blob -> BlobStoreHelper.delete(storage, BlobId.of(BUCKET_NAME, blob.name())));
    }

    @Test
    public void testUpload() {
        Blob blob = BlobStoreHelper.uploadBlob(storage, BUCKET_NAME, blobName, this.getClass().getResourceAsStream(fileName), false);
        assertNotNull(blob);
    }

    @Test
    public void testList() {
        BlobStoreHelper.uploadBlob(storage, BUCKET_NAME, blobName, this.getClass().getResourceAsStream(fileName), false);
        Iterator<Blob> blobIterator = BlobStoreHelper.listAllBlobsRecursively(storage, BUCKET_NAME, directory);
        assertEquals(true, blobIterator.hasNext());
        Blob result = blobIterator.next();
        assertEquals(blobName, result.name());
        assertEquals(false, blobIterator.hasNext());
    }

    @Test
    public void testDownload() throws IOException {
        BlobStoreHelper.uploadBlob(storage, BUCKET_NAME, blobName, this.getClass().getResourceAsStream(fileName), false);
        InputStream inputStream = BlobStoreHelper.getBlob(storage, BUCKET_NAME, blobName);
        byte[] result = ByteStreams.toByteArray(inputStream);
        byte[] expected = ByteStreams.toByteArray(this.getClass().getResourceAsStream(fileName));
        assertEquals(expected.length, result.length);
        assertTrue(Arrays.equals(expected, result));
    }

    @Test
    public void testDelete() throws IOException {
        BlobStoreHelper.uploadBlob(storage, BUCKET_NAME, blobName, this.getClass().getResourceAsStream(fileName), false);
        BlobStoreHelper.delete(storage, BlobId.of(BUCKET_NAME, blobName));
        Iterator<Blob> blobIterator = BlobStoreHelper.listAllBlobsRecursively(storage, BUCKET_NAME, directory);
        assertEquals(false, blobIterator.hasNext());
    }

}