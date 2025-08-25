package org.rutebanken.helper.stopplace.changelog.repository;

public class StopPlaceFetchException extends RuntimeException {

  public StopPlaceFetchException(String stopPlaceId, Exception exception) {
    super(
      String.format(
        "Failed to get update for id %s from stop place repository",
        stopPlaceId
      ),
      exception
    );
  }
}
