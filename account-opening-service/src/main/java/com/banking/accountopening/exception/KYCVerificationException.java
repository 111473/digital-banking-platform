package com.banking.accountopening.exception;

/**
 * Exception thrown when KYC verification requirements are not met.
 */
public class KYCVerificationException extends RuntimeException {

    public KYCVerificationException(String message) {
        super(message);
    }

    public KYCVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}