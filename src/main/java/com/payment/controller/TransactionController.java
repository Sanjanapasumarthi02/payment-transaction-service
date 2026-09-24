package com.payment.controller;

import com.payment.dto.TransactionRequest;
import com.payment.dto.TransactionResponse;
import com.payment.dto.UserDTO;
import com.payment.model.TransactionStatus;
import com.payment.service.TransactionService;
import com.payment.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller exposing user management and transaction endpoints.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final UserService userService;
    private final TransactionService transactionService;

    public TransactionController(UserService userService, TransactionService transactionService) {
        this.userService = userService;
        this.transactionService = transactionService;
    }

    // ----------------------------------------------------
    // User Endpoints
    // ----------------------------------------------------

    /**
     * POST /api/users/register
     * Register a new user account with default balance (10,000) or specified balance.
     */
    @PostMapping("/users/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody UserDTO userDto) {
        UserDTO createdUser = userService.registerUser(
                userDto.getUsername(),
                userDto.getEmail(),
                userDto.getBalance()
        );
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * GET /api/users/{id}
     * Get user details by ID.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable("id") Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/users
     * List all registered users.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // ----------------------------------------------------
    // Transaction Endpoints
    // ----------------------------------------------------

    /**
     * POST /api/transactions/send
     * Initiate a money transfer between two users.
     */
    @PostMapping("/transactions/send")
    public ResponseEntity<TransactionResponse> sendTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.initiateTransaction(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * GET /api/transactions/history/{userId}
     * Retrieve complete transaction history (both sent and received) for a given user.
     */
    @GetMapping("/transactions/history/{userId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(@PathVariable("userId") Long userId) {
        List<TransactionResponse> history = transactionService.getTransactionHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/transactions/{transactionId}
     * Retrieve transaction details by transaction ID.
     */
    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable("transactionId") String transactionId) {
        TransactionResponse response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/transactions/status/{status}
     * Retrieve transactions filtered by status (PENDING, COMPLETED, FAILED).
     */
    @GetMapping("/transactions/status/{status}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByStatus(@PathVariable("status") TransactionStatus status) {
        List<TransactionResponse> transactions = transactionService.getTransactionsByStatus(status);
        return ResponseEntity.ok(transactions);
    }
}
