package org.entur.pubsub.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.loader.tools.JavaExecutable;
import org.springframework.boot.loader.tools.RunProcess;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Profile("google-pubsub-emulator")
@Component
public class EnturGooglePubSubEmulatorRunner {

    private static final Logger logger = LoggerFactory.getLogger(EnturGooglePubSubEmulatorRunner.class);

    @Value("${entur.pubsub.emulator.path:target/pubsub-emulator/pubsub-emulator-0.1-SNAPSHOT.jar}")
    private String pathToEmulator;

    @Value("${spring.cloud.gcp.pubsub.emulatorHost:localhost:8089}")
    private String emulatorHostAndPort;


    private final RunProcess pubsubEmulatorProcess;

    public EnturGooglePubSubEmulatorRunner() {
        pubsubEmulatorProcess = new RunProcess(new JavaExecutable().toString());

        Runtime.getRuntime()
                .addShutdownHook(new Thread(new RunProcessKiller(pubsubEmulatorProcess)));
    }

    @EventListener
    @Order(50)
    public void handleContextRefreshed(ContextRefreshedEvent contextRefreshedEvent) throws InterruptedException {
        try {
            logger.info("Starting Google PubSub Emulator");
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
            Thread.sleep(2000);
            logger.info("Started Google PubSub Emulator");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventListener
    @Order(150)
    public void handleContextClosedEvent(ContextClosedEvent contextClosedEvent) {
        if (pubsubEmulatorProcess != null) {
            logger.info("Stopping Google PubSub Emulator");
            pubsubEmulatorProcess.kill();
            logger.info("Stopped Google PubSub Emulator");

        }
    }


    private static final class RunProcessKiller implements Runnable {

        private final RunProcess runProcess;

        private RunProcessKiller(RunProcess runProcess) {
            this.runProcess = runProcess;
        }

        @Override
        public void run() {
            logger.info("Stopping Google PubSub Emulator on VM shutdown");
            this.runProcess.kill();
            logger.info("Stopped Google PubSub Emulator on VM shutdown");
        }

    }
}
