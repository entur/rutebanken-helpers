package org.rutebanken.helper.trace;


import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DummyTraceTest {
    private static Logger log = LoggerFactory.getLogger(DummyTraceTest.class);

    @Test
    public void dummy() throws IOException {
        log.info("Performing mock call");
        new DummyTrace().call();

        // export GOOGLE_APPLICATION_CREDENTIALS=$HOME/Private/Carbon-eb93b021fde2.json
        // or
        // String clientSecretsFile = System.getProperty("clientSecretsFile");
        //GoogleCredentials.fromStream(new FileInputStream(clientSecretsFile))

        log.info("Done");
    }
}