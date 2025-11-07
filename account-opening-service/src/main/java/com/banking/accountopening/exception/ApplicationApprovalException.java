package com.banking.accountopening.exception;

/**
 * Exception thrown when attempting to approve an application with invalid status.
 */
public class ApplicationApprovalException extends RuntimeException {

    public ApplicationApprovalException(String message) {
        super(message);
    }

    public ApplicationApprovalException(String message, Throwable cause) {
        super(message, cause);
    }
}