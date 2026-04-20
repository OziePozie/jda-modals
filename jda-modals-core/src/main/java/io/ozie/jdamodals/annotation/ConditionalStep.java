package io.ozie.jdamodals.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes a field's step conditional based on a previously collected value.
 * When the condition is not met, the entire step containing this field is skipped.
 *
 * <p>All fields in the same step should share the same condition (same {@code dependsOn}
 * and {@code havingValue}). If multiple fields in a step have different conditions,
 * ALL conditions must be satisfied for the step to be shown.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Modal(id = "survey", title = "Survey")
 * public class SurveyModal extends ModalModel {
 *
 *     @SelectMenu(type = STRING, label = "Do you have experience?", step = 1)
 *     @SelectOption(label = "Yes", value = "yes")
 *     @SelectOption(label = "No", value = "no")
 *     private String hasExperience;
 *
 *     @TextInput(label = "Describe your experience", step = 2)
 *     @ConditionalStep(dependsOn = "hasExperience", havingValue = "yes")
 *     private String experienceDetails;
 *
 *     @TextInput(label = "Your email", step = 3)
 *     private String email;
 * }
 * }</pre>
 *
 * <p>If the user selects "No" in step 1, step 2 is skipped entirely and the wizard
 * proceeds directly to step 3.</p>
 *
 * @see StepResolver for programmatic step resolution with complex logic
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalStep {

    /**
     * The field name whose value determines whether this step is shown.
     * Must reference a field from a previous step.
     */
    String dependsOn();

    /**
     * The value(s) that the dependent field must have for this step to be shown.
     * If multiple values are specified, any match will satisfy the condition (OR logic).
     */
    String[] havingValue();

    /**
     * If true, inverts the condition: the step is shown when the dependent field
     * does NOT have any of the specified values.
     */
    boolean negate() default false;
}
