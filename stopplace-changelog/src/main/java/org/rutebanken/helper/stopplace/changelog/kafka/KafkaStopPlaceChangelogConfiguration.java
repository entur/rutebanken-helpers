package org.rutebanken.helper.stopplace.changelog.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

@Configuration
public class KafkaStopPlaceChangelogConfiguration {

  @Value(
    "${org.rutebanken.helper.stopplace.changelog.kafka.bootstrap-servers:}"
  )
  private String bootstrapServers;

  @Value("${org.rutebanken.helper.stopplace.changelog.kafka.group-id:}")
  private String groupId;

  @Value(
    "${org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-url:}"
  )
  private String schemaRegistryUrl;

  @Value(
    "${org.rutebanken.helper.stopplace.changelog.kafka.security-protocol:}"
  )
  private String securityProtocol;

  @Value("${org.rutebanken.helper.stopplace.changelog.kafka.sasl-mechanism:}")
  private String saslMechanism;

  @Value("${org.rutebanken.helper.stopplace.changelog.kafka.sasl-jaas-config:}")
  private String saslJaasConfig;

  @Value(
    "${org.rutebanken.helper.stopplace.changelog.kafka.schema-registry-basic-auth-user-info:}"
  )
  private String schemaRegistryBasicAuthUserInfo;

  @Bean("publicationTimeRecordFilterStrategy")
  @ConditionalOnMissingBean(name = "publicationTimeRecordFilterStrategy")
  public RecordFilterStrategy<String, StopPlaceChangelogEvent> recordFilterStrategy() {
    return new PublicationTimeRecordFilterStrategy(Instant.now());
  }

  @Bean("finder")
  @ConditionalOnMissingBean(name = "finder")
  public PartitionFinder partitionFinder(
    ConsumerFactory<String, ?> consumerFactory
  ) {
    return new PartitionFinder(consumerFactory);
  }

  @Bean
  @ConditionalOnMissingBean
  public ConsumerFactory<String, Object> consumerFactory() {
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

  @Bean
  @ConditionalOnMissingBean
  public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
    ConsumerFactory<String, Object> consumerFactory
  ) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
      new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    return factory;
  }
}
