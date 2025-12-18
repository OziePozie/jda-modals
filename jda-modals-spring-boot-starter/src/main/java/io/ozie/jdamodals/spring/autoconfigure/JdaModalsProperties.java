package io.ozie.jdamodals.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for JDA Modals.
 */
@ConfigurationProperties(prefix = "jda.modals")
public class JdaModalsProperties {

    /**
     * Session time-to-live. Default is 15 minutes.
     */
    private Duration sessionTtl = Duration.ofMinutes(15);

    /**
     * Interval for session cleanup task. Default is 5 minutes.
     */
    private Duration cleanupInterval = Duration.ofMinutes(5);

    public Duration getSessionTtl() {
        return sessionTtl;
    }

    public void setSessionTtl(Duration sessionTtl) {
        this.sessionTtl = sessionTtl;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }
}
