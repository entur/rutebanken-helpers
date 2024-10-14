package org.rutebanken.helper.storage.model;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * Descriptor type for representing a blob to store with all properties which may be attached to it.
 * @param name
 * @param inputStream
 * @param contentType
 * @param metadata
 */
public record BlobDescriptor(String name,
                             InputStream inputStream,
                             Optional<String> contentType,
                             Optional<Map<String, String>> metadata) {

    /**
     * Convenience constructor for building <code>BlobDescriptor</code> with just the required parameters.
     * @param name
     * @param inputStream
     */
    public BlobDescriptor(String name, InputStream inputStream) {
        this(name, inputStream, Optional.empty(), Optional.empty());
    }
}
