package org.rutebanken.helper.stopplace.changelog;

import java.io.InputStream;

public interface StopPlaceChangelogListener {
  default void onStopPlaceCreated(String id, InputStream stopPlace) {}
  default void onStopPlaceUpdated(String id, InputStream stopPlace) {}
  default void onStopPlaceDeactivated(String id, InputStream stopPlace) {}
  default void onStopPlaceDeleted(String id) {}
}
