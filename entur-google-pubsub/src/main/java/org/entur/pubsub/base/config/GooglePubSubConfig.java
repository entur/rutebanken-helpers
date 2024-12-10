package org.entur.pubsub.base.config;

import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Specific Spring configuration.
 * There is currently none defined, the class is kept for backward compatibility.
 */
@Configuration
@AutoConfigureBefore(GcpPubSubAutoConfiguration.class)
@ComponentScan("org.entur.pubsub")
public class GooglePubSubConfig {}
