package org.rutebanken.helper.gcp;

public class BlobConcurrentUpdateException extends RuntimeException {

    public BlobConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlobConcurrentUpdateException(Throwable cause) {
        super(cause);
    }

    public BlobConcurrentUpdateException(String message) {
        super(message);
    }
}
