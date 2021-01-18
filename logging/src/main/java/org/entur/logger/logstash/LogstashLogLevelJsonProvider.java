package org.entur.logger.logstash;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;

import java.io.IOException;

/**
 * LogLevel provider that maps the WARN severity to "WARNING".
 * The default implementation maps it to "WARN", which is mot compatible with StackDriver.
 */
public class LogstashLogLevelJsonProvider extends LogLevelJsonProvider {

    private static final String DEBUG = "DEBUG";
    private static final String ERROR = "ERROR";
    private static final String INFO = "INFO";
    private static final String WARNING = "WARNING";
    private static final String SEVERITY_FIELD_NAME = "severity";

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event)
            throws IOException {
        JsonWritingUtils.writeStringField(generator, SEVERITY_FIELD_NAME,
                getCustomLogLevel(event));
    }

    private String getCustomLogLevel(ILoggingEvent event) {
        if (event.getLevel() == Level.ALL) {
            return Level.ALL.toString();
        }
        if (event.getLevel() == Level.DEBUG) {
            return DEBUG;
        }
        if (event.getLevel() == Level.ERROR) {
            return ERROR;
        }
        if (event.getLevel() == Level.INFO) {
            return INFO;
        }
        if (event.getLevel() == Level.WARN) {
            return WARNING;
        }
        return "";
    }
}

