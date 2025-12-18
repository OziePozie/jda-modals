package io.ozie.jdamodals.exception;

/**
 * Exception thrown when modal mapping fails.
 */
public class ModalMappingException extends RuntimeException {

    public ModalMappingException(String message) {
        super(message);
    }

    public ModalMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
