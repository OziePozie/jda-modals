package io.ozie.jdamodals.i18n;

import java.util.Locale;

/**
 * Interface for providing localized messages in the modal framework.
 * Implement this interface to customize messages or provide translations.
 */
public interface ModalMessages {

    /**
     * Message shown when a modal session has expired.
     */
    String sessionExpired(Locale locale);

    /**
     * Label for the "Continue" button in multi-step modals.
     */
    String continueButton(Locale locale);

    /**
     * Message shown after completing a step in a multi-step modal.
     *
     * @param currentStep the step that was just completed
     * @param totalSteps  the total number of steps
     */
    String stepCompleted(int currentStep, int totalSteps, Locale locale);

    /**
     * Error message shown when modal processing fails.
     */
    String error(Locale locale);
}
