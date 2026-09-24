package com.payment.repository;

import com.payment.model.Transaction;
import com.payment.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Transaction entity.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Find all transactions where the user is either the sender or the recipient.
     *
     * @param userId the ID of the user
     * @return List of transactions involving the user, ordered by creation date descending
     */
    @Query("SELECT t FROM Transaction t WHERE t.senderId = :userId OR t.recipientId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByUserId(@Param("userId") Long userId);

    /**
     * Find all transactions by sender ID and transaction status.
     *
     * @param senderId the sender's user ID
     * @param status the status of the transaction
     * @return List of matching transactions
     */
    List<Transaction> findBySenderIdAndStatus(Long senderId, TransactionStatus status);

    /**
     * Find all transactions by status.
     *
     * @param status the status of the transaction
     * @return List of matching transactions
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Convenience method to find a transaction by String UUID representation.
     *
     * @param id the String representation of UUID
     * @return Optional containing the Transaction if found
     */
    default Optional<Transaction> findById(String id) {
        try {
            return findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
