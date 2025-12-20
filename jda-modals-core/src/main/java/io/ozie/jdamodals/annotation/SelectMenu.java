package io.ozie.jdamodals.annotation;

import net.dv8tion.jda.api.entities.channel.ChannelType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a SelectMenu component in a Modal.
 * Supports user, role, channel, mentionable, and string selection menus.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SelectMenu {

    /**
     * Component ID. If empty, field name will be used.
     */
    String id() default "";

    /**
     * Label displayed above the select menu.
     */
    String label();

    /**
     * The type of select menu.
     */
    SelectMenuType type();

    /**
     * Placeholder text shown when nothing is selected.
     */
    String placeholder() default "";

    /**
     * Whether selection is required.
     */
    boolean required() default true;

    /**
     * Minimum number of values that must be selected.
     */
    int minValues() default 1;

    /**
     * Maximum number of values that can be selected.
     * Set higher than 1 for multi-select. Maximum is 25.
     */
    int maxValues() default 1;

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

    /**
     * Options for STRING type select menus.
     * Required when type is {@link SelectMenuType#STRING}.
     */
    SelectOption[] options() default {};

    /**
     * Channel types to show for CHANNEL type select menus.
     * If empty, all channel types are shown.
     */
    ChannelType[] channelTypes() default {};
}
