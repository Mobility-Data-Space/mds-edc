package io.nexyo.edp.extensions.exceptions;

/**
 * Custom exception for EDP-related errors.
 */
public class EdpException extends RuntimeException {

    /**
     * Constructs a new EdpException with the specified detail message.
     *
     * @param message the detail message
     */
    public EdpException(String message) {
        super(message);
    }

    /**
     * Constructs a new EdpException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public EdpException(String message, Throwable cause) {
        super(message, cause);
    }
}
