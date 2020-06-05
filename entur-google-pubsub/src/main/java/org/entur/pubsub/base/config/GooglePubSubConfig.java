package org.entur.pubsub.base.config;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Specific Spring configuration.
 * There is currently none defined, the class is kept for backward compatiblity.
 */
@Configuration
@AutoConfigureBefore(GcpPubSubAutoConfiguration.class)
@ComponentScan("org.entur.pubsub")
public class GooglePubSubConfig {


}
