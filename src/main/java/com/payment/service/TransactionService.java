package com.payment.service;

import com.payment.dto.TransactionRequest;
import com.payment.dto.TransactionResponse;
import com.payment.exception.InsufficientFundsException;
import com.payment.exception.UserNotFoundException;
import com.payment.exception.ValidationException;
import com.payment.model.Transaction;
import com.payment.model.TransactionStatus;
import com.payment.model.User;
import com.payment.repository.TransactionRepository;
import com.payment.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing payment transaction processing, balance transfers, and history.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Initiates and executes an atomic payment transaction from sender to recipient.
     *
     * @param senderId user ID of the sender
     * @param recipientId user ID of the recipient
     * @param amount the transfer amount (must be positive)
     * @param description optional notes/description for the transaction
     * @return TransactionResponse containing details of the completed transaction
     */
    @Transactional
    public TransactionResponse initiateTransaction(Long senderId, Long recipientId, BigDecimal amount, String description) {
        // 1. Validate inputs
        if (senderId == null || recipientId == null) {
            throw new ValidationException("Sender and recipient IDs must not be null");
        }

        if (senderId.equals(recipientId)) {
            throw new ValidationException("Sender cannot send money to themselves");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Transaction amount must be greater than zero");
        }

        // 2. Validate sender exists
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));

        // 3. Validate recipient exists
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException(recipientId));

        // 4. Check sufficient balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(senderId, sender.getBalance(), amount);
        }

        // 5. Atomically deduct from sender and credit recipient
        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount));

        userRepository.save(sender);
        userRepository.save(recipient);

        // 6. Record and save completed transaction
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                senderId,
                recipientId,
                amount,
                description,
                TransactionStatus.COMPLETED,
                now,
                now
        );

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionResponse.fromEntity(savedTransaction);
    }

    /**
     * Overloaded method accepting TransactionRequest DTO.
     */
    @Transactional
    public TransactionResponse initiateTransaction(TransactionRequest request) {
        if (request == null) {
            throw new ValidationException("Transaction request must not be null");
        }
        return initiateTransaction(
                request.getSenderId(),
                request.getRecipientId(),
                request.getAmount(),
                request.getDescription()
        );
    }

    /**
     * Retrieves all transactions involving a specific user (as sender or recipient).
     *
     * @param userId the user ID
     * @return List of transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long userId) {
        if (userId == null) {
            throw new ValidationException("User ID must not be null");
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return transactionRepository.findByUserId(userId).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves details of a specific transaction by its UUID.
     *
     * @param transactionId the UUID of the transaction
     * @return TransactionResponse
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID transactionId) {
        if (transactionId == null) {
            throw new ValidationException("Transaction ID must not be null");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found with ID: " + transactionId));

        return TransactionResponse.fromEntity(transaction);
    }

    /**
     * Retrieves details of a specific transaction by its String UUID representation.
     *
     * @param transactionId the string UUID
     * @return TransactionResponse
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new ValidationException("Transaction ID must not be empty");
        }
        try {
            UUID uuid = UUID.fromString(transactionId.trim());
            return getTransactionById(uuid);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid UUID format: " + transactionId);
        }
    }

    /**
     * Retrieves transactions by their status.
     *
     * @param status the transaction status
     * @return List of matching transactions
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByStatus(TransactionStatus status) {
        if (status == null) {
            throw new ValidationException("Status must not be null");
        }
        return transactionRepository.findByStatus(status).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
