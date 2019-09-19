package org.entur.pubsub.base;

import org.entur.pubsub.base.config.GooglePubSubConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GooglePubSubConfig.class)
public class AppTest {
    public static void main(String[] args) {
        new SpringApplicationBuilder(AppTest.class).run(args);
    }
}
