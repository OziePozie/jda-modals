package io.ozie.jdamodals.localization;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Default implementation of {@link ModalMessages} using Java ResourceBundle.
 * Loads messages from jda-modals-messages.properties files.
 *
 * <p>To add custom translations, create files in your classpath:</p>
 * <ul>
 *   <li>jda-modals-messages.properties (default/English)</li>
 *   <li>jda-modals-messages_ru.properties (Russian)</li>
 *   <li>jda-modals-messages_de.properties (German)</li>
 *   <li>etc.</li>
 * </ul>
 */
public class DefaultModalMessages implements ModalMessages {

    private static final String BUNDLE_NAME = "jda-modals-messages";

    @Override
    public String sessionExpired(Locale locale) {
        return getMessage("jda.modals.sessionExpired", locale);
    }

    @Override
    public String continueButton(Locale locale) {
        return getMessage("jda.modals.continueButton", locale);
    }

    @Override
    public String stepCompleted(int currentStep, int totalSteps, Locale locale) {
        String pattern = getMessage("jda.modals.stepCompleted", locale);
        return MessageFormat.format(pattern, currentStep, totalSteps);
    }

    @Override
    public String error(Locale locale) {
        return getMessage("jda.modals.error", locale);
    }

    private String getMessage(String key, Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale != null ? locale : Locale.ENGLISH);
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
                return bundle.getString(key);
            } catch (MissingResourceException e2) {
                return key;
            }
        }
    }
}
