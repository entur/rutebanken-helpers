package org.entur.ror.helpers.stopplace.changelog.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.kafka.KafkaStopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.kafka.PartitionFinder;
import org.rutebanken.helper.stopplace.changelog.kafka.PublicationTimeRecordFilterStrategy;
import org.rutebanken.helper.stopplace.changelog.repository.StopPlaceRepository;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@EnableKafka
@Configuration
@ConditionalOnProperty(
  name = "org.rutebanken.helper.stopplace.changelog.enabled",
  havingValue = "true"
)
public class StopPlaceChangelogConfiguration {

  /**
   * Creates a default WebClient bean with sensible defaults.
   *
   * <p>Configuration:
   * <ul>
   *   <li>Connection timeout: 30 seconds</li>
   *   <li>Response timeout: 60 seconds</li>
   *   <li>Max in-memory buffer: 10MB (for large NeTEx responses)</li>
   * </ul>
   *
   * @return a configured WebClient instance
   */
  @Bean("tiamatWebClient")
  @ConditionalOnMissingBean(name = "tiamatWebClient")
  public WebClient webClient() {
    HttpClient httpClient = HttpClient
      .create()
      .responseTimeout(Duration.ofSeconds(60));

    // Allow larger responses for NeTEx data (30MB)
    ExchangeStrategies exchangeStrategies = ExchangeStrategies
      .builder()
      .codecs(configurer ->
        configurer.defaultCodecs().maxInMemorySize(30 * 1024 * 1024) // 30MB
      )
      .build();

    return WebClient
      .builder()
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .exchangeStrategies(exchangeStrategies)
      .build();
  }

  @Bean
  public StopPlaceRepository tiamatStopPlaceRepository(
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

  @Bean
  StopPlaceChangelog stopPlaceChangelog(
    StopPlaceRepository stopPlaceRepository
  ) {
    return new KafkaStopPlaceChangelog(stopPlaceRepository);
  }

  @Bean("publicationTimeRecordFilterStrategy")
  @ConditionalOnMissingBean(name = "publicationTimeRecordFilterStrategy")
  public RecordFilterStrategy<String, StopPlaceChangelogEvent> recordFilterStrategy() {
    return new PublicationTimeRecordFilterStrategy(Instant.now());
  }

  @Bean("stopPlaceChangelogPartitionFinder")
  @ConditionalOnMissingBean(name = "stopPlaceChangelogPartitionFinder")
  public PartitionFinder partitionFinder(
    ConsumerFactory<String, ?> tiamatChangelogConsumerFactory
  ) {
    return new PartitionFinder(tiamatChangelogConsumerFactory);
  }

  @Bean("tiamatChangelogConsumerFactory")
  @ConditionalOnMissingBean(name = "tiamatChangelogConsumerFactory")
  public ConsumerFactory<String, Object> consumerFactory(
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.kafka.bootstrap-servers:}"
    ) String bootstrapServers,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.kafka.group-id:}"
    ) String groupId,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-url:}"
    ) String schemaRegistryUrl,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.kafka.security-protocol:}"
    ) String securityProtocol,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.kafka.sasl-mechanism:}"
    ) String saslMechanism,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.kafka.sasl-jaas-config:}"
    ) String saslJaasConfig,
    @Value(
      "${org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-basic-auth-user-info:}"
    ) String schemaRegistryBasicAuthUserInfo
  ) {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    // Only set group ID if explicitly provided
    if (groupId != null && !groupId.isEmpty()) {
      props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    }

    props.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      StringDeserializer.class
    );
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      KafkaAvroDeserializer.class
    );
    props.put(
      KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
      schemaRegistryUrl
    );
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

    if (
      schemaRegistryBasicAuthUserInfo != null &&
      !schemaRegistryBasicAuthUserInfo.isEmpty()
    ) {
      props.put("basic.auth.credentials.source", "USER_INFO");
      props.put("basic.auth.user.info", schemaRegistryBasicAuthUserInfo);
    }

    if (securityProtocol != null && !securityProtocol.isEmpty()) {
      props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
    }

    if (saslMechanism != null && !saslMechanism.isEmpty()) {
      props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
    }

    if (saslJaasConfig != null && !saslJaasConfig.isEmpty()) {
      props.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
    }

    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new DefaultKafkaConsumerFactory<>(props);
  }

  @Bean("tiamatChangelogListenerContainerFactory")
  @ConditionalOnMissingBean(name = "tiamatChangelogListenerContainerFactory")
  public ConcurrentKafkaListenerContainerFactory<String, Object> containerFactory(
    ConsumerFactory<String, Object> tiamatChangelogConsumerFactory
  ) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
      new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(tiamatChangelogConsumerFactory);
    return factory;
  }
}
