package io.ozie.jdamodals.spring.autoconfigure;

import io.ozie.jdamodals.localization.DefaultModalMessages;
import io.ozie.jdamodals.localization.ModalMessages;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

/**
 * Spring MessageSource-backed implementation of {@link ModalMessages}.
 * Falls back to default messages if MessageSource doesn't have the key.
 */
public class SpringModalMessages implements ModalMessages {

    private final MessageSource messageSource;
    private final DefaultModalMessages fallback = new DefaultModalMessages();

    public SpringModalMessages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String sessionExpired(Locale locale) {
        return getMessage("jda.modals.sessionExpired", null, locale, fallback.sessionExpired(locale));
    }

    @Override
    public String continueButton(Locale locale) {
        return getMessage("jda.modals.continueButton", null, locale, fallback.continueButton(locale));
    }

    @Override
    public String stepCompleted(int currentStep, int totalSteps, Locale locale) {
        String defaultMessage = fallback.stepCompleted(currentStep, totalSteps, locale);
        return getMessage("jda.modals.stepCompleted", new Object[]{currentStep, totalSteps}, locale, defaultMessage);
    }

    @Override
    public String error(Locale locale) {
        return getMessage("jda.modals.error", null, locale, fallback.error(locale));
    }

    private String getMessage(String key, Object[] args, Locale locale, String defaultMessage) {
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException e) {
            return defaultMessage;
        }
    }
}
