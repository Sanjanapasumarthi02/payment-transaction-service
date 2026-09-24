package com.payment.exception;

/**
 * Thrown when business validation fails (e.g. invalid amount, sender equals recipient).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
