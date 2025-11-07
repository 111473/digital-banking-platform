package com.banking.accountopening.exception;

/**
 * Exception thrown when attempting to start review on an application that is not SUBMITTED.
 */
public class ApplicationNotSubmittedException extends RuntimeException {

    public ApplicationNotSubmittedException(String message) {
        super(message);
    }

    public ApplicationNotSubmittedException(String message, Throwable cause) {
        super(message, cause);
    }
}
