package org.entur.logger;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LogstashLoggerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogstashLoggerTest.class);

    @Test
    void testLogger() {
        LOGGER.warn("test logger");
    }
}
