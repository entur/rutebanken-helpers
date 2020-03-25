package org.entur.pubsub.camel.config;

import org.apache.camel.CamelContext;
import org.entur.pubsub.base.config.GooglePubSubConfig;
import org.entur.pubsub.camel.EnturGooglePubSubComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Register the Camel PubSub component.
 */
@Configuration
@Import(GooglePubSubConfig.class)
public class GooglePubSubCamelComponentConfig {


    @Autowired
    public void registerPubsubComponent(CamelContext camelContext, EnturGooglePubSubComponent enturGooglePubsub) {
        camelContext.addComponent("entur-google-pubsub", enturGooglePubsub);
    }


    @Bean
    public EnturGooglePubSubComponent googlePubsubComponent() {
        return new EnturGooglePubSubComponent();
    }


}
