package com.payment;

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
import com.payment.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User sender;
    private User recipient;

    @BeforeEach
    void setUp() {
        sender = new User(1L, "alice", "alice@example.com", new BigDecimal("5000.00"), LocalDateTime.now());
        recipient = new User(2L, "bob", "bob@example.com", new BigDecimal("5000.00"), LocalDateTime.now());
    }

    @Test
    @DisplayName("Should successfully initiate transaction when inputs are valid and balance is sufficient")
    void testSuccessfulTransaction() {
        // Arrange
        Long senderId = 1L;
        Long recipientId = 2L;
        BigDecimal amount = new BigDecimal("1500.00");
        String description = "Payment for services";

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransactionResponse response = transactionService.initiateTransaction(senderId, recipientId, amount, description);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(senderId, response.getSenderId());
        assertEquals(recipientId, response.getRecipientId());
        assertEquals(amount, response.getAmount());
        assertEquals(description, response.getDescription());
        assertEquals(TransactionStatus.COMPLETED, response.getStatus());

        // Check balances
        assertEquals(new BigDecimal("3500.00"), sender.getBalance());
        assertEquals(new BigDecimal("6500.00"), recipient.getBalance());

        // Verify repository interactions
        verify(userRepository).save(sender);
        verify(userRepository).save(recipient);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when sender balance is less than amount")
    void testInsufficientFundsScenario() {
        // Arrange
        Long senderId = 1L;
        Long recipientId = 2L;
        BigDecimal amount = new BigDecimal("6000.00"); // Sender only has 5000.00

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        // Act & Assert
        InsufficientFundsException exception = assertThrows(
                InsufficientFundsException.class,
                () -> transactionService.initiateTransaction(senderId, recipientId, amount, "Too much")
        );

        assertNotNull(exception.getMessage());
        // Verify no changes were saved
        verify(userRepository, never()).save(any(User.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when sender does not exist")
    void testSenderNotFoundScenario() {
        // Arrange
        Long nonExistentSenderId = 99L;
        Long recipientId = 2L;
        BigDecimal amount = new BigDecimal("500.00");

        when(userRepository.findById(nonExistentSenderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> transactionService.initiateTransaction(nonExistentSenderId, recipientId, amount, "Transfer")
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when recipient does not exist")
    void testRecipientNotFoundScenario() {
        // Arrange
        Long senderId = 1L;
        Long nonExistentRecipientId = 99L;
        BigDecimal amount = new BigDecimal("500.00");

        when(userRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(userRepository.findById(nonExistentRecipientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UserNotFoundException.class,
                () -> transactionService.initiateTransaction(senderId, nonExistentRecipientId, amount, "Transfer")
        );

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when sender is recipient")
    void testSenderEqualsRecipientScenario() {
        // Arrange
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("100.00");

        // Act & Assert
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> transactionService.initiateTransaction(userId, userId, amount, "Self transfer")
        );

        assertEquals("Sender cannot send money to themselves", exception.getMessage());
        verify(userRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when amount is zero or negative")
    void testInvalidAmountScenario() {
        // Arrange
        Long senderId = 1L;
        Long recipientId = 2L;
        BigDecimal zeroAmount = BigDecimal.ZERO;
        BigDecimal negativeAmount = new BigDecimal("-100.00");

        // Act & Assert
        assertThrows(
                ValidationException.class,
                () -> transactionService.initiateTransaction(senderId, recipientId, zeroAmount, "Zero")
        );

        assertThrows(
                ValidationException.class,
                () -> transactionService.initiateTransaction(senderId, recipientId, negativeAmount, "Negative")
        );

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should retrieve transaction history for existing user")
    void testGetTransactionHistory() {
        // Arrange
        Long userId = 1L;
        Transaction tx1 = new Transaction(UUID.randomUUID(), 1L, 2L, new BigDecimal("100.00"), "test1",
                TransactionStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now());
        Transaction tx2 = new Transaction(UUID.randomUUID(), 3L, 1L, new BigDecimal("200.00"), "test2",
                TransactionStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.existsById(userId)).thenReturn(true);
        when(transactionRepository.findByUserId(userId)).thenReturn(List.of(tx1, tx2));

        // Act
        List<TransactionResponse> history = transactionService.getTransactionHistory(userId);

        // Assert
        assertEquals(2, history.size());
        assertEquals(new BigDecimal("100.00"), history.get(0).getAmount());
        assertEquals(new BigDecimal("200.00"), history.get(1).getAmount());
    }

    @Test
    @DisplayName("Should retrieve transaction by valid UUID string")
    void testGetTransactionByIdString() {
        // Arrange
        UUID txId = UUID.randomUUID();
        Transaction tx = new Transaction(txId, 1L, 2L, new BigDecimal("500.00"), "Dinner",
                TransactionStatus.COMPLETED, LocalDateTime.now(), LocalDateTime.now());

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        // Act
        TransactionResponse response = transactionService.getTransactionById(txId.toString());

        // Assert
        assertNotNull(response);
        assertEquals(txId, response.getId());
        assertEquals(new BigDecimal("500.00"), response.getAmount());
    }
}
