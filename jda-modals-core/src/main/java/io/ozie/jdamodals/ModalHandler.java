package io.ozie.jdamodals;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

/**
 * Interface for handling modal submissions.
 * Implement this interface to automatically handle modal events.
 *
 * @param <T> the modal model type
 */
public interface ModalHandler<T extends ModalModel> {

    /**
     * Returns the modal model class this handler processes.
     */
    Class<T> getModalClass();

    /**
     * Handles the modal submission.
     *
     * @param event the original modal interaction event (for replying, etc.)
     * @param modal the populated modal model instance
     */
    void handle(ModalInteractionEvent event, T modal);

    /**
     * Returns a custom step resolver for complex conditional branching.
     * When provided, the resolver takes priority over {@link io.ozie.jdamodals.annotation.ConditionalStep}
     * annotations.
     *
     * @return a step resolver, or {@code null} to use annotation-based resolution (default)
     */
    default StepResolver getStepResolver() {
        return null;
    }
}
