package com.banking.accountopening.exception;

/**
 * Exception thrown when attempting to submit an application that is not in PENDING status.
 */
public class ApplicationPendingException extends RuntimeException {

    public ApplicationPendingException(String message) {
        super(message);
    }

    public ApplicationPendingException(String message, Throwable cause) {
        super(message, cause);
    }
}