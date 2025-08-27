package org.entur.ror.helpers.stopplace.changelog.config;

import org.rutebanken.helper.stopplace.changelog.repository.StopPlaceRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

public class StopPlaceRepositoryConfig {

    @Bean
    public StopPlaceRepository stopPlaceRepository(
            @Qualifier("tiamatWebClient") WebClient webClient,
            @Value(
                    "${org.rutebanken.helper.stopplace.changelog.repository.url:}"
            ) String tiamatUrl,
            @Value(
                    "${org.rutebanken.helper.stopplace.changelog.repository.topographicPlaceExportMode:RELEVANT}"
            ) String topographicPlaceExportMode,
            @Value(
                    "${org.rutebanken.helper.stopplace.changelog.repository.tariffZoneExportMode:RELEVANT}"
            ) String tariffZoneExportMode,
            @Value(
                    "${org.rutebanken.helper.stopplace.changelog.repository.groupOfTariffZonesExportMode:RELEVANT}"
            ) String groupOfTariffZonesExportMode,
            @Value(
                    "${org.rutebanken.helper.stopplace.changelog.repository.fareZoneExportMode:RELEVANT}"
            ) String fareZoneExportMode,
            @Value(
                    "${org.rutebanken.helper.stopplace.changelog.repository.groupOfStopPlacesExportMode:RELEVANT}"
            ) String groupOfStopPlacesExportMode,
            @Value(
                    "${org.rutebanken.helper.stopplace.changelog.repository.allVersions:true}"
            ) boolean allVersions
    ) {
        return new StopPlaceRepository(
                webClient,
                tiamatUrl,
                topographicPlaceExportMode,
                tariffZoneExportMode,
                groupOfTariffZonesExportMode,
                fareZoneExportMode,
                groupOfStopPlacesExportMode,
                allVersions
        );
    }
}
