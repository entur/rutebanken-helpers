package org.entur.pubsub.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(classes = TestApp.class)
@ActiveProfiles({"google-pubsub-emulator"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BasePubSubIntegrationTest {

}
