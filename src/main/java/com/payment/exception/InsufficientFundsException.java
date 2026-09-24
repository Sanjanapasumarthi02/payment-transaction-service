package com.payment.exception;

import java.math.BigDecimal;

/**
 * Thrown when an account does not have sufficient funds to complete a transfer.
 */
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(Long userId, BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(String.format("User %d has insufficient funds (Current balance: %s, Required: %s)",
                userId, currentBalance, requiredAmount));
    }
}
