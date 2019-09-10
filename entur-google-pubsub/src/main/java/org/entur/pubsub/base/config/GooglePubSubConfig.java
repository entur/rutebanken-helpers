package org.entur.pubsub.base.config;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configure a common thread pool for managing deadline extensions.
 */
@Configuration
@AutoConfigureBefore(GcpPubSubAutoConfiguration.class)
@ComponentScan("org.entur.pubsub")
public class GooglePubSubConfig {

    /**
     * Number of threads performing ack deadline extension.
     */
    @Value("${entur.pubsub.subscriber.system-threads:5}")
    private Integer subscriberSystemThreads;

    @Bean("pubsubSystemThreadPool")
    public ThreadPoolTaskScheduler pubsubSystemThreadPool() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(subscriberSystemThreads);
        scheduler.setThreadNamePrefix("gcp-pubsub-system");
        return scheduler;
    }

    @Bean("subscriberSystemExecutorProvider")
    public ExecutorProvider systemExecutorProvider(
            @Qualifier("pubsubSystemThreadPool") ThreadPoolTaskScheduler scheduler) {
        return FixedExecutorProvider.create(scheduler.getScheduledExecutor());
    }



}
