package org.rutebanken.helper.storage;

public class BlobAlreadyExistsException extends RuntimeException {

    public BlobAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlobAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public BlobAlreadyExistsException(String message) {
        super(message);
    }
}
