package org.entur.pubsub.base.config;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Component scan for the consumers and admin. Imported explicitly by each application.
 */
@Configuration
// Repeat the filter @SpringBootApplication applies, or every @TestConfiguration here leaks into
// consumers' test contexts.
@ComponentScan(
  value = "org.entur.pubsub",
  excludeFilters = @ComponentScan.Filter(
    type = FilterType.CUSTOM,
    classes = TypeExcludeFilter.class
  )
)
public class GooglePubSubConfig {}
