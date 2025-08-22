package org.rutebanken.helper.stopplace.changelog;

public interface StopPlaceChangelog {
  void registerStopPlaceChangelogListener(StopPlaceChangelogListener listener);
  void unregisterStopPlaceChangelogListener(
    StopPlaceChangelogListener listener
  );
}
