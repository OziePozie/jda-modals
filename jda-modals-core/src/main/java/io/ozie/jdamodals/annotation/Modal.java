package io.ozie.jdamodals.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Discord Modal model.
 * The annotated class should extend {@link io.ozie.jdamodals.ModalModel}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Modal {

    /**
     * Unique identifier for the modal.
     */
    String id();

    /**
     * Title displayed at the top of the modal.
     */
    String title();
}
