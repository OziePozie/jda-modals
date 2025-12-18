package io.ozie.jdamodals;

import io.ozie.jdamodals.annotation.Modal;

/**
 * Base class for all modal models.
 * Extend this class and annotate with {@link Modal}
 * to create a declarative modal definition.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Modal(id = "registration", title = "Registration")
 * public class RegistrationModal extends ModalModel {
 *
 *     @TextInput(label = "Name", placeholder = "Enter your name")
 *     private String name;
 *
 *     @Attachment(label = "Screenshot")
 *     private Message.Attachment screenshot;
 *
 *     // getters...
 * }
 * }</pre>
 */
public abstract class ModalModel {

    /**
     * Returns the modal ID from the @Modal annotation.
     */
    public String getModalId() {
        var annotation = this.getClass().getAnnotation(Modal.class);
        if (annotation == null) {
            throw new IllegalStateException("Class must be annotated with @Modal");
        }
        return annotation.id();
    }

    /**
     * Returns the modal title from the @Modal annotation.
     */
    public String getModalTitle() {
        var annotation = this.getClass().getAnnotation(Modal.class);
        if (annotation == null) {
            throw new IllegalStateException("Class must be annotated with @Modal");
        }
        return annotation.title();
    }
}
