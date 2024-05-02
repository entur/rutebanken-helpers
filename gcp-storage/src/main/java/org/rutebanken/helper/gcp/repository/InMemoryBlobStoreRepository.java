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

package org.rutebanken.helper.gcp.repository;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.rutebanken.helper.gcp.BlobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple memory-based blob store repository for testing purpose.
 */
public class InMemoryBlobStoreRepository implements BlobStoreRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryBlobStoreRepository.class);


    /**
     * Autowire a shared map so that each prototype bean can access blobs from other containers.
     * This is needed for {@link BlobStoreRepository#copyBlob(String, String, String, String)}
     */
    private final Map<String, Map<String, byte[]>> blobsInContainers;

    private String containerName;

    public InMemoryBlobStoreRepository(Map<String, Map<String, byte[]>> blobsInContainers) {
        this.blobsInContainers = blobsInContainers;
    }

    protected Map<String, byte[]> getBlobsForCurrentContainer() {
        return getBlobsForContainer(containerName);
    }

    protected Map<String, byte[]> getBlobsForContainer(String aContainer) {
        return blobsInContainers.computeIfAbsent(aContainer, k -> new ConcurrentHashMap<>());
    }

    @Override
    public InputStream getBlob(String objectName) {
        LOGGER.debug("get blob called in in-memory blob store");
        byte[] data = getBlobsForCurrentContainer().get(objectName);
        return (data == null) ? null : new ByteArrayInputStream(data);
    }

    @Override
    public long uploadBlob(String objectName, InputStream inputStream, String contentType) {
        return uploadBlob(objectName, inputStream);
    }

    @Override
    public long uploadBlob(String objectName, InputStream inputStream) {
        try {
            LOGGER.debug("upload blob called in in-memory blob store");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();
            getBlobsForCurrentContainer().put(objectName, data);
            // no blob versioning for the in memory implementation
            return 0;
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

    @Override
    public void copyBlob(String sourceContainerName, String sourceObjectName, String targetContainerName, String targetObjectName) {
        copyVersionedBlob(sourceContainerName, sourceObjectName, null, targetContainerName, targetObjectName);
    }

    @Override
    public void copyVersionedBlob(String sourceContainerName, String sourceObjectName, Long sourceVersion, String targetContainerName, String targetObjectName) {
        byte[] sourceData = getBlobsForContainer(sourceContainerName).get(sourceObjectName);
        getBlobsForContainer(targetContainerName).put(targetObjectName, sourceData);
    }

    @Override
    public void copyAllBlobs(String sourceContainerName, String prefix, String targetContainerName, String targetPrefix) {
        getBlobsForContainer(sourceContainerName).keySet()
                .stream()
                .filter(blobName -> blobName.startsWith(prefix))
                .forEach(blobName -> copyBlob(sourceContainerName, blobName, targetContainerName, blobName.replace(prefix, targetPrefix)));
    }

    @Override
    public boolean delete(String objectName) {
        getBlobsForCurrentContainer().remove(objectName);
        return true;
    }

    @Override
    public boolean deleteAllFilesInFolder(String folder) {
        getBlobsForCurrentContainer().keySet().stream()
                .filter(fileName -> fileName.startsWith(folder))
                .forEach(this::delete);
        return true;
    }

    @Override
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String containerName() {
        return containerName;
    }

}
