package org.entur.pubsub.base;

public class EnturGooglePubSubException extends RuntimeException {

  public EnturGooglePubSubException(String message) {
    super(message);
  }

  public EnturGooglePubSubException(Exception cause) {
    super(cause);
  }
}
