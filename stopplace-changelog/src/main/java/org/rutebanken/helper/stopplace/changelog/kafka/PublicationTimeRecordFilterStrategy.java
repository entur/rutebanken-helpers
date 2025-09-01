package org.rutebanken.helper.stopplace.changelog.kafka;

import java.time.Instant;

/**
 * Filter StopPlaceChangelogEvent based on the provided publication time.
 */
public class PublicationTimeRecordFilterStrategy
  extends BasePublicationTimeRecordFilterStrategy {

  private final Instant publicationTime;

  public PublicationTimeRecordFilterStrategy(Instant publicationTime) {
    this.publicationTime = publicationTime;
  }

  @Override
  protected Instant publicationTime() {
    return publicationTime;
  }
}
