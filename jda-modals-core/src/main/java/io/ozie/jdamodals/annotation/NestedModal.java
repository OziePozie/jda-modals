package io.ozie.jdamodals.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a nested modal model for composition.
 * Allows reusing modal field definitions across different modals.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Modal(id = "duo_reg", title = "Duo Registration")
 * public class DuoRegistrationModal extends ModalModel {
 *     @NestedModal(prefix = "p1", titleSuffix = " - Player 1")
 *     private PlayerInfo player1;
 *
 *     @NestedModal(prefix = "p2", titleSuffix = " - Player 2")
 *     private PlayerInfo player2;
 * }
 * }</pre>
 *
 * <p>The field type must extend {@link io.ozie.jdamodals.ModalModel}.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedModal {

    /**
     * Prefix for field IDs to avoid collisions between nested models.
     * Example: prefix = "p1" transforms field "name" to "p1_name".
     */
    String prefix();

    /**
     * Suffix appended to the modal title for steps of this nested model.
     * Example: titleSuffix = " - Player 1" with base title "Registration"
     * results in "Registration - Player 1".
     */
    String titleSuffix() default "";

    /**
     * Order of this nested modal relative to other fields and nested modals.
     * Lower values appear first. Default is 0.
     */
    int order() default 0;
}
