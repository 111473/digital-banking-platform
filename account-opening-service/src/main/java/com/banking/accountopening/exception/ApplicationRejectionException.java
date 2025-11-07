package com.banking.accountopening.exception;

/**
 * Exception thrown when attempting to reject an application with invalid status.
 */
public class ApplicationRejectionException extends RuntimeException {

    public ApplicationRejectionException(String message) {
        super(message);
    }

    public ApplicationRejectionException(String message, Throwable cause) {
        super(message, cause);
    }
}