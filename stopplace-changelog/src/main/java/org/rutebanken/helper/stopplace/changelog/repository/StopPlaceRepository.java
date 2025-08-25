package org.rutebanken.helper.stopplace.changelog.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class StopPlaceRepository {

  private static final Logger logger = LoggerFactory.getLogger(
    StopPlaceRepository.class
  );
  private final RestTemplate tiamatClient;
  private final String tiamatUrl;

  @Autowired
  public StopPlaceRepository(
    RestTemplate tiamatClient,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.repository.url:}"
    ) String tiamatUrl
  ) {
    this.tiamatClient = tiamatClient;
    this.tiamatUrl = tiamatUrl;
  }

  public InputStream getStopPlaceUpdate(String stopPlaceId) {
    String stopPlaceUrl = UriComponentsBuilder
      .fromHttpUrl(tiamatUrl + "/netex")
      .queryParam("idList", "{idList}")
      .queryParam("topographicPlaceExportMode", "{topographicPlaceExportMode}")
      .queryParam("tariffZoneExportMode", "{tariffZoneExportMode}")
      .queryParam(
        "groupOfTariffZonesExportMode",
        "{groupOfTariffZonesExportMode}"
      )
      .queryParam("fareZoneExportMode", "{fareZoneExportMode}")
      .queryParam(
        "groupOfStopPlacesExportMode",
        "{groupOfStopPlacesExportMode}"
      )
      .queryParam("allVersions", "{allVersions}")
      .queryParam("size", "{size}")
      .encode()
      .toUriString();

    try {
      var response = tiamatClient.exchange(
        stopPlaceUrl,
        HttpMethod.GET,
        null,
        Resource.class,
        Map.of(
          "idList",
          stopPlaceId,
          "topographicPlaceExportMode",
          "RELEVANT",
          "tariffZoneExportMode",
          "RELEVANT",
          "groupOfTariffZonesExportMode",
          "RELEVANT",
          "fareZoneExportMode",
          "RELEVANT",
          "groupOfStopPlacesExportMode",
          "RELEVANT",
          "allVersions",
          true,
          "size",
          Integer.MAX_VALUE
        )
      );
      return Objects.requireNonNull(response.getBody()).getInputStream();
    } catch (RestClientException | IOException exception) {
      throw new StopPlaceFetchException(stopPlaceId, exception);
    }
  }
}
