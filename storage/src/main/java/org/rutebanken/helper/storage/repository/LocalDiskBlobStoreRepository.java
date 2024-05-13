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
 */

package org.rutebanken.helper.storage.repository;

import org.rutebanken.helper.storage.BlobAlreadyExistsException;
import org.rutebanken.helper.storage.BlobStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Simple file-based blob store repository for testing purpose.
 */
public class LocalDiskBlobStoreRepository implements BlobStoreRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalDiskBlobStoreRepository.class);

    private final String baseFolder;

    private String containerName;

    public LocalDiskBlobStoreRepository(String baseFolder) {
        this.baseFolder = baseFolder;
    }

    protected String getContainerFolder() {
        return baseFolder + File.separator + containerName;
    }

    @Override
    public InputStream getBlob(String objectName) {
        LOGGER.debug("get blob called in local-disk blob store on {}", objectName);
        Path path = Paths.get(getContainerFolder()).resolve(objectName);
        if (!path.toFile().exists()) {
            LOGGER.debug("getBlob(): File not found in local-disk blob store: {} ", path);
            return null;
        }
        LOGGER.debug("getBlob(): File found in local-disk blob store: {} ", path);
        try {
            // converted as ByteArrayInputStream so that Camel stream cache can reopen it
            // since ByteArrayInputStream.close() does nothing
            return new ByteArrayInputStream(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

    @Override
    public long uploadBlob(String objectName, InputStream inputStream) {
        LOGGER.debug("Upload blob called in local-disk blob store on {}", objectName);
        try {
            Path localPath = Paths.get(objectName);
            Path parentDirectory = localPath.getParent();
            Path folder = parentDirectory == null ? Paths.get(getContainerFolder()) : Paths.get(getContainerFolder()).resolve(parentDirectory);
            Files.createDirectories(folder);

            Path fullPath = Paths.get(getContainerFolder()).resolve(localPath);
            Files.deleteIfExists(fullPath);

            Files.copy(inputStream, fullPath);
            // no blob versioning for the Local disk implementation
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
        try {
            Path sourcePath = Path.of(baseFolder, sourceContainerName, sourceObjectName);
            Path targetPath = Path.of(baseFolder, targetContainerName, targetObjectName);
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

    @Override
    public void copyAllBlobs(String sourceContainerName, String prefix, String targetContainerName, String targetPrefix) {
        // no-op implementation for local disk blobstore
    }

    @Override
    public long uploadBlob(String objectName, InputStream inputStream, String contentType) {
        return uploadBlob(objectName, inputStream);
    }

    @Override
    public long uploadNewBlob(String objectName, InputStream inputStream) {
        if(getBlob(objectName) != null) {
            throw new BlobAlreadyExistsException("The blob with name '" + objectName + "' already exists");
        }
        return uploadBlob(objectName, inputStream);
    }

    @Override
    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    @Override
    public boolean delete(String objectName) {
        LOGGER.debug("Delete blob called in local-disk blob store on: {}", objectName);
        Path path = Paths.get(getContainerFolder()).resolve(objectName);
        if (!path.toFile().exists()) {
            LOGGER.debug("delete(): File not found in local-disk blob store: {} ", path);
            return false;
        }
        try {
            Files.delete(path);
            return true;
        } catch (IOException e) {
            throw new BlobStoreException(e);
        }
    }

    @Override
    public boolean deleteAllFilesInFolder(String folder) {
        Path folderToDelete = Paths.get(getContainerFolder()).resolve(folder);
        if (folderToDelete.toFile().isDirectory()) {
            try (Stream<Path> paths = Files.walk(folderToDelete)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new BlobStoreException(e);
                            }
                        });
                return true;
            } catch (IOException e) {
                throw new BlobStoreException(e);
            }
        }
        return false;
    }

}
