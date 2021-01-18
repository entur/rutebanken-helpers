package org.entur.logger.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;

/**
 * Custom Logstash encoder that encodes properly message severity for the level "WARNING".
 * The default implementation maps WARNING-messages to the severity "WARN", while StackDriver
 * expects the severity to be "WARNING".
 */
public class LogstashEncoder extends LoggingEventCompositeJsonEncoder {

    @Override
    public void start() {
        JsonProviders<ILoggingEvent> providers = getProviders();
        providers.addProvider(new LogstashLogLevelJsonProvider());
        super.start();
    }
}