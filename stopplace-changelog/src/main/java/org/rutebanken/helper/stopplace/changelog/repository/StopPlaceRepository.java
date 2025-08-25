package org.rutebanken.helper.stopplace.changelog.repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Repository for fetching stop place data from the remote API.
 *
 * <p>This repository uses Spring's WebClient to fetch stop place data in NeTEx format
 * from the configured repository URL. The data is fetched synchronously and returned
 * as an InputStream for processing.</p>
 *
 * @see StopPlaceFetchException
 * @since 5.41.0
 * @author Entur
 */
@Component("stopPlaceChangelogStopPlaceRepository")
public class StopPlaceRepository {

  private static final Logger logger = LoggerFactory.getLogger(
    StopPlaceRepository.class
  );

  private final WebClient webClient;

  @Value("${org.rutebanken.helper.stopplace.changelog.repository.allVersions:true}")
  private boolean allVersions;

  @Autowired
  public StopPlaceRepository(
    WebClient webClient,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.repository.url:}"
    ) String tiamatUrl
  ) {
    this.webClient = webClient.mutate().baseUrl(tiamatUrl).build();
  }

  /**
   * Fetches stop place data for the given stop place ID.
   *
   * <p>This method retrieves the complete NeTEx representation of a stop place,
   * including all versions and relevant related entities (topographic places,
   * tariff zones, fare zones, etc.).</p>
   *
   * @param stopPlaceId the NeTEx ID of the stop place to fetch
   * @return an InputStream containing the NeTEx XML data
   * @throws StopPlaceFetchException if the stop place cannot be fetched
   */
  public InputStream getStopPlaceUpdate(String stopPlaceId) {
    logger.debug("Fetching stop place update for ID: {}", stopPlaceId);

    try {
      byte[] responseBytes = webClient
        .get()
        .uri(uriBuilder ->
          uriBuilder
            .path("/netex")
            .queryParam("idList", stopPlaceId)
            .queryParam("topographicPlaceExportMode", "RELEVANT")
            .queryParam("tariffZoneExportMode", "RELEVANT")
            .queryParam("groupOfTariffZonesExportMode", "RELEVANT")
            .queryParam("fareZoneExportMode", "RELEVANT")
            .queryParam("groupOfStopPlacesExportMode", "RELEVANT")
            .queryParam("allVersions", allVersions)
            .queryParam("size", Integer.MAX_VALUE)
            .build()
        )
        .retrieve()
        .bodyToMono(byte[].class)
        .block(); // Synchronous call for backward compatibility

      if (responseBytes == null || responseBytes.length == 0) {
        throw new StopPlaceFetchException(
          stopPlaceId,
          new IllegalStateException("Empty response from repository")
        );
      }

      logger.debug(
        "Successfully fetched stop place {} ({} bytes)",
        stopPlaceId,
        responseBytes.length
      );

      return new ByteArrayInputStream(responseBytes);
    } catch (WebClientResponseException e) {
      logger.error(
        "HTTP error fetching stop place {}: {} - {}",
        stopPlaceId,
        e.getStatusCode(),
        e.getResponseBodyAsString()
      );
      throw new StopPlaceFetchException(stopPlaceId, e);
    } catch (Exception e) {
      logger.error("Error fetching stop place {}", stopPlaceId, e);
      throw new StopPlaceFetchException(stopPlaceId, e);
    }
  }
}
