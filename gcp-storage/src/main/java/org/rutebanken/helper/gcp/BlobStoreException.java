package org.rutebanken.helper.gcp;

public class BlobStoreException extends RuntimeException {

    public BlobStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlobStoreException(Throwable cause) {
        super(cause);
    }

    public BlobStoreException(String message) {
        super(message);
    }
}
