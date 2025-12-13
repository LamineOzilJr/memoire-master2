package sn.groupeisi.leaveworkflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled task execution in the application.
 * This allows Spring to process @Scheduled annotations on service methods.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Scheduled tasks will be processed by Spring
}

