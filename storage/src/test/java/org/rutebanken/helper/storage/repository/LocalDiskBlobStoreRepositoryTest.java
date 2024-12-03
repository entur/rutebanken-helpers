package org.rutebanken.helper.storage.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class LocalDiskBlobStoreRepositoryTest {

    public static final String BLOB_NAME = "blobName";
    public static final byte[] BLOB_CONTENT = "content".getBytes();

    @TempDir
    File tempDirectory;

    @Test
    void testUploadExistAndGet() throws IOException {
        LocalDiskBlobStoreRepository repository = new LocalDiskBlobStoreRepository(tempDirectory.getAbsolutePath());
        repository.uploadBlob(BLOB_NAME, new ByteArrayInputStream(BLOB_CONTENT));
        assertTrue(repository.exist(BLOB_NAME));
        InputStream blob = repository.getBlob(BLOB_NAME);
        assertNotNull(blob);
        assertArrayEquals(BLOB_CONTENT, blob.readAllBytes());
    }

}