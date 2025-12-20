package io.ozie.jdamodals.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines an option for a STRING type SelectMenu.
 * Used within {@link SelectMenu#options()}.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface SelectOption {

    /**
     * The value returned when this option is selected.
     */
    String value();

    /**
     * The display label shown to users.
     */
    String label();

    /**
     * Optional description shown under the label.
     */
    String description() default "";

    /**
     * Optional emoji to display with this option.
     * Can be a unicode emoji (e.g., "🎮") or a custom emoji format (e.g., ":name:id").
     */
    String emoji() default "";

    /**
     * Whether this option is selected by default.
     */
    boolean isDefault() default false;
}
