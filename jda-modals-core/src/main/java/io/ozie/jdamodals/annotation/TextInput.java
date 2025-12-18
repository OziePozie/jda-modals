package io.ozie.jdamodals.annotation;

import net.dv8tion.jda.api.components.textinput.TextInputStyle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a TextInput component in a Modal.
 * Field type should be String.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TextInput {

    /**
     * Component ID. If empty, field name will be used.
     */
    String id() default "";

    /**
     * Label displayed above the input field.
     */
    String label();

    /**
     * Placeholder text shown when the input is empty.
     */
    String placeholder() default "";

    /**
     * Input style: SHORT (single line) or PARAGRAPH (multiline).
     */
    TextInputStyle style() default TextInputStyle.SHORT;

    /**
     * Whether this field is required.
     */
    boolean required() default true;

    /**
     * Minimum input length. -1 means no limit.
     */
    int minLength() default -1;

    /**
     * Maximum input length. -1 means no limit.
     */
    int maxLength() default -1;

    /**
     * Default value for the input.
     */
    String defaultValue() default "";

    /**
     * Order of the field in the modal (lower = higher).
     */
    int order() default 0;

    /**
     * Step number for multi-step modals (wizard pattern).
     * Fields with the same step will be shown in one modal.
     * Default is 1 (single-step modal).
     */
    int step() default 1;
}
