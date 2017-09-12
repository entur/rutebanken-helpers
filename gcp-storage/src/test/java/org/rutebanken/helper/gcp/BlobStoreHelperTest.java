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

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.common.io.ByteStreams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class BlobStoreHelperTest {

    private static final String BUCKET_NAME = "marduk-test";
    private static final String credentialPath = "/home/tomgag/.ssh/Carbon-ef49cabc6d04.json";
    private static final String directory = "test/";

    private static Storage storage;
    private final String fileName = "avinor-netex-dummy.zip";
    private final String blobName = directory + fileName;

    @BeforeAll
    public static void beforeAll() throws IOException {
        storage = BlobStoreHelper.getStorage(credentialPath, "carbon-1287");
    }

    @BeforeEach
    public void beforeEach() {
        // Eating our own dog food here
        Iterator<Blob> blobIterator = BlobStoreHelper.listAllBlobsRecursively(storage, BUCKET_NAME, directory);
        blobIterator.forEachRemaining(blob -> BlobStoreHelper.delete(storage, BlobId.of(BUCKET_NAME, blob.getName())));
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
        System.out.println(result.getBlobId());
        assertEquals(blobName, result.getName());
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