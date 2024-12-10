package org.rutebanken.helper.storage;

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
