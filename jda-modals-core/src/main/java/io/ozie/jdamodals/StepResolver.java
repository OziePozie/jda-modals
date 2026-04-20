package io.ozie.jdamodals;

import io.ozie.jdamodals.session.ModalSession;

/**
 * Programmatic step resolver for complex conditional branching in multi-step modals.
 * Implement this interface when {@link io.ozie.jdamodals.annotation.ConditionalStep}
 * annotations are not expressive enough for your branching logic.
 *
 * <p>Example usage in a handler:</p>
 * <pre>{@code
 * public class SurveyHandler implements ModalHandler<SurveyModal> {
 *
 *     @Override
 *     public StepResolver getStepResolver() {
 *         return (session, currentStep, totalSteps) -> {
 *             if (currentStep == 1) {
 *                 String answer = (String) session.getFieldValue("hasExperience");
 *                 return "yes".equals(answer) ? 2 : 3;
 *             }
 *             return currentStep + 1;
 *         };
 *     }
 * }
 * }</pre>
 *
 * <p>When both a {@code StepResolver} and {@code @ConditionalStep} annotations are present,
 * the {@code StepResolver} takes priority.</p>
 *
 * @see io.ozie.jdamodals.annotation.ConditionalStep for declarative step conditions
 */
@FunctionalInterface
public interface StepResolver {

    /**
     * Determines the next step to show after the current step is completed.
     *
     * <p>Return a step number greater than {@code totalSteps} to skip all remaining
     * steps and finalize the modal immediately.</p>
     *
     * @param session    the current session with all collected field values
     * @param currentStep the step that was just completed (1-based)
     * @param totalSteps  the total number of defined steps
     * @return the next step number to show, or a value {@code > totalSteps} to finalize
     */
    int resolveNextStep(ModalSession session, int currentStep, int totalSteps);
}
