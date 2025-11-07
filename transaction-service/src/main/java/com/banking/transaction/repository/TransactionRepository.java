package com.banking.transaction.repository;

import com.banking.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByTransactionId(String transactionId);

    List<TransactionEntity> findByAccountNumberOrderByTransactionDateDesc(Integer accountNumber);

    // ✅ NEW: Get most recent transaction for balance calculation
    Optional<TransactionEntity> findTopByAccountNumberOrderByTransactionDateDesc(Integer accountNumber);

    List<TransactionEntity> findByCustomerId(Integer customerId);

    @Query("SELECT t FROM TransactionEntity t WHERE t.accountNumber = :accountNumber " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<TransactionEntity> findTransactionsByDateRange(
            @Param("accountNumber") Integer accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}