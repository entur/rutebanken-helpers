package org.entur.pubsub.camel;

import org.apache.camel.Endpoint;
import org.apache.camel.support.LifecycleStrategySupport;
import org.entur.pubsub.base.EnturGooglePubSubAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EnturGooglePubSubSubscriptionCreator extends LifecycleStrategySupport {


    @Autowired
    private EnturGooglePubSubAdmin enturGooglePubSubAdmin;

    @Override
    public void onEndpointAdd(Endpoint endpoint) {
        if (endpoint instanceof EnturGooglePubSubEndpoint) {
            enturGooglePubSubAdmin.createSubscriptionIfMissing(((EnturGooglePubSubEndpoint) endpoint).getDestinationName());
        }
    }

}
