package com.payment.service;

import com.payment.dto.UserDTO;
import com.payment.exception.UserNotFoundException;
import com.payment.exception.ValidationException;
import com.payment.model.User;
import com.payment.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing users and accounts.
 */
@Service
public class UserService {

    public static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("10000.00");

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user with the specified username, email, and initial balance.
     * If initial balance is null, defaults to 10000.
     *
     * @param username the desired username
     * @param email the user's email
     * @param initialBalance the starting balance (defaults to 10000 if null)
     * @return UserDTO representing the created user
     */
    @Transactional
    public UserDTO registerUser(String username, String email, BigDecimal initialBalance) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be empty");
        }
        if (userRepository.existsByUsername(username.trim())) {
            throw new ValidationException("Username '" + username + "' is already taken");
        }

        BigDecimal balanceToSet = (initialBalance != null) ? initialBalance : DEFAULT_INITIAL_BALANCE;
        if (balanceToSet.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Initial balance cannot be negative");
        }

        User newUser = new User(
                username.trim(),
                email.trim(),
                balanceToSet,
                LocalDateTime.now()
        );

        User savedUser = userRepository.save(newUser);
        return UserDTO.fromEntity(savedUser);
    }

    /**
     * Convenience method to register user with default balance (10000).
     */
    @Transactional
    public UserDTO registerUser(String username, String email) {
        return registerUser(username, email, DEFAULT_INITIAL_BALANCE);
    }

    /**
     * Fetches user details by user ID.
     *
     * @param id the user ID
     * @return UserDTO representing the user
     * @throws UserNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = findUserEntityById(id);
        return UserDTO.fromEntity(user);
    }

    /**
     * Retrieves all registered users.
     *
     * @return List of UserDTOs
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to retrieve the User JPA entity by ID.
     *
     * @param id user ID
     * @return User entity
     * @throws UserNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public User findUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
