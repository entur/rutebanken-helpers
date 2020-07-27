package org.entur.pubsub.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.loader.tools.JavaExecutable;
import org.springframework.boot.loader.tools.RunProcess;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;


/**
 * Start a PubSub emulator in a separate process. Used for automated testing.
 * The emulator is started as early as possible after the Spring context initialization is complete,
 * and stopped as late as possible on context shutdown.
 *
 */
@Profile("google-pubsub-emulator")
@Component
public class EnturGooglePubSubEmulatorRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnturGooglePubSubEmulatorRunner.class);

    @Value("${entur.pubsub.emulator.path:target/pubsub-emulator/pubsub-emulator.jar}")
    private String pathToEmulator;

    @Value("${spring.cloud.gcp.pubsub.emulatorHost:localhost:8089}")
    private String emulatorHostAndPort;

    @Autowired
    private PubSubAdmin pubSubAdmin;


    private final RunProcess pubsubEmulatorProcess;

    public EnturGooglePubSubEmulatorRunner() {
        pubsubEmulatorProcess = new RunProcess(new JavaExecutable().toString());

        Runtime.getRuntime()
                .addShutdownHook(new Thread(new RunProcessKiller(pubsubEmulatorProcess)));
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void handleContextRefreshed(ContextRefreshedEvent contextRefreshedEvent) throws InterruptedException {
        try {
            LOGGER.info("Starting Google PubSub Emulator");
            File emulatorExecutable = new File(pathToEmulator);
            if(!emulatorExecutable.exists()) {
                throw new IllegalStateException("Google PubSub Emulator not found at " + pathToEmulator + ".\n The emulator can be installed with the following command:\n gcloud -q components install pubsub-emulator ");
            }

            String[] splitEmulatorHostAndPort = emulatorHostAndPort.split(":");
            if(splitEmulatorHostAndPort.length != 2) {
                throw new IllegalStateException("The property 'spring.cloud.gcp.pubsub.emulatorHost' should follow the pattern 'host:port'");
            }
            String emulatorPort = splitEmulatorHostAndPort[1];
            pubsubEmulatorProcess.run(false, "-jar", pathToEmulator, "--port=" + emulatorPort);
            // wait for the emulator to start up
            boolean ready = false;
            do {
                try {
                    pubSubAdmin.listTopics();
                    ready = true;
                } catch (Exception e) {
                    LOGGER.info("Google PubSub Emulator initialization in progress...");
                    Thread.sleep(2000);
                }
            } while (!ready);
            LOGGER.info("Started Google PubSub Emulator");
        } catch (IOException e) {
            throw new EnturGooglePubSubException(e);
        }
    }

    @EventListener
    public void handleContextClosedEvent(ContextClosedEvent contextClosedEvent) {
        if (pubsubEmulatorProcess != null) {
            LOGGER.info("Stopping Google PubSub Emulator");
            pubsubEmulatorProcess.kill();
            LOGGER.info("Stopped Google PubSub Emulator");

        }
    }


    private static final class RunProcessKiller implements Runnable {

        private final RunProcess runProcess;

        private RunProcessKiller(RunProcess runProcess) {
            this.runProcess = runProcess;
        }

        @Override
        public void run() {
            LOGGER.info("Stopping Google PubSub Emulator on VM shutdown");
            this.runProcess.kill();
            LOGGER.info("Stopped Google PubSub Emulator on VM shutdown");
        }

    }
}
