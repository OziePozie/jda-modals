package io.ozie.jdamodals.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as an AttachmentUpload component in a Modal.
 * Field type should be {@link net.dv8tion.jda.api.entities.Message.Attachment}
 * or {@link java.util.List} of Attachments.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Attachment {

    /**
     * Component ID. If empty, field name will be used.
     */
    String id() default "";

    /**
     * Label displayed above the attachment upload.
     */
    String label();

    /**
     * Whether this field is required.
     */
    boolean required() default true;

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
