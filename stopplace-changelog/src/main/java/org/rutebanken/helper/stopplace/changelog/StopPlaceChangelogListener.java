package org.rutebanken.helper.stopplace.changelog;

import java.io.InputStream;

public interface StopPlaceChangelogListener {
  void onStopPlaceCreated(String id, InputStream stopPlace);
  void onStopPlaceUpdated(String id, InputStream stopPlace);
  void onStopPlaceDeactivated(String id, InputStream stopPlace);
  void onStopPlaceDeleted(String id);
}
