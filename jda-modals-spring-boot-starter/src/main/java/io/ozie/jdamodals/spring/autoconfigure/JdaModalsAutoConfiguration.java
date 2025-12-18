package io.ozie.jdamodals.spring.autoconfigure;

import io.ozie.jdamodals.ModalDispatcher;
import io.ozie.jdamodals.ModalHandler;
import io.ozie.jdamodals.ModalMapper;
import io.ozie.jdamodals.i18n.DefaultModalMessages;
import io.ozie.jdamodals.i18n.ModalMessages;
import io.ozie.jdamodals.session.ModalSessionStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Auto-configuration for JDA Modals framework.
 * Automatically configures ModalMapper, ModalSessionStorage, and ModalDispatcher
 * when JDA is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(name = "net.dv8tion.jda.api.JDA")
@EnableConfigurationProperties(JdaModalsProperties.class)
@EnableScheduling
public class JdaModalsAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JdaModalsAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public ModalMapper modalMapper() {
        return new ModalMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModalSessionStorage modalSessionStorage(JdaModalsProperties properties) {
        return new ModalSessionStorage(properties.getSessionTtl());
    }

    @Bean
    @ConditionalOnMissingBean
    public ModalMessages modalMessages(ObjectProvider<MessageSource> messageSourceProvider) {
        MessageSource messageSource = messageSourceProvider.getIfAvailable();
        if (messageSource != null) {
            log.debug("Using Spring MessageSource for modal messages");
            return new SpringModalMessages(messageSource);
        }
        log.debug("Using default modal messages");
        return new DefaultModalMessages();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModalDispatcher modalDispatcher(
            List<ModalHandler<?>> handlers,
            ModalMapper mapper,
            ModalSessionStorage sessionStorage,
            ModalMessages messages) {
        return new ModalDispatcher(handlers, mapper, sessionStorage, messages);
    }

    @Bean
    public ModalSessionCleanupTask modalSessionCleanupTask(
            ModalSessionStorage sessionStorage,
            JdaModalsProperties properties) {
        return new ModalSessionCleanupTask(sessionStorage, properties);
    }

    /**
     * Scheduled task for cleaning up expired modal sessions.
     */
    public static class ModalSessionCleanupTask {

        private final ModalSessionStorage sessionStorage;
        private final long cleanupIntervalMs;

        public ModalSessionCleanupTask(ModalSessionStorage sessionStorage, JdaModalsProperties properties) {
            this.sessionStorage = sessionStorage;
            this.cleanupIntervalMs = properties.getCleanupInterval().toMillis();
        }

        @Scheduled(fixedRateString = "${jda.modals.cleanup-interval:300000}")
        public void cleanup() {
            sessionStorage.cleanupExpiredSessions();
        }
    }
}
