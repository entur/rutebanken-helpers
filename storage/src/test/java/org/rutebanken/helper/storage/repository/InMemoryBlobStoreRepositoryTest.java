package org.rutebanken.helper.storage.repository;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
class InMemoryBlobStoreRepositoryTest {

    public static final String BLOB_NAME = "blobName";
    public static final byte[] BLOB_CONTENT = "content".getBytes();

    @Test
    void testUploadExistAndGet() throws IOException {
        InMemoryBlobStoreRepository repository = new InMemoryBlobStoreRepository(new HashMap<>());
        repository.uploadBlob(BLOB_NAME, new ByteArrayInputStream(BLOB_CONTENT));
        assertTrue(repository.exist(BLOB_NAME));
        InputStream blob = repository.getBlob(BLOB_NAME);
        assertNotNull(blob);
        assertArrayEquals(BLOB_CONTENT, blob.readAllBytes());
    }
  
}