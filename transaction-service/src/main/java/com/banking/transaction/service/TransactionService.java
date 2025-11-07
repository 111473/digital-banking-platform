package com.banking.transaction.service;

import com.banking.events.BankAccountCreatedEvent;
import com.banking.events.TransactionCreatedEvent;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.entity.TransactionEntity;
import com.banking.transaction.enums.TransactionStatus;
import com.banking.transaction.enums.TransactionType;
import com.banking.transaction.exception.ResourceNotFoundException;
import com.banking.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing financial transactions.
 *
 * ✅ Updated to work with Kafka events instead of direct BankAccountEntity dependency
 */
@Slf4j
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();

    public TransactionService(TransactionRepository transactionRepository,
                              KafkaTemplate<String, Object> kafkaTemplate) {
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * ✅ NEW: Kafka listener - Records initial transaction when bank account is created
     */
    @KafkaListener(
            topics = "bank-account-created",
            groupId = "transaction-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleBankAccountCreated(BankAccountCreatedEvent event) {
        log.info("📨 Received BankAccountCreatedEvent for accountNumber: {}",
                event.getAccountNumber());

        try {
            // Record initial deposit transaction if there's an initial balance
            if (event.getInitialBalance() != null && event.getInitialBalance() > 0) {
                TransactionEntity transaction = createTransaction(
                        event.getAccountNumber(),
                        event.getCustomerId(),
                        TransactionType.DEPOSIT,
                        event.getInitialBalance(),
                        0.0,  // Balance before (new account)
                        event.getInitialBalance(),  // Balance after
                        "Initial deposit - Account opening",
                        "INITIAL-" + event.getAccountNumber()
                );

                log.info("✅ Recorded initial transaction: transactionId={}, accountNumber={}",
                        transaction.getTransactionId(), event.getAccountNumber());

                // Publish transaction event
                publishTransactionCreatedEvent(transaction, event.getCustomerId());
            }

        } catch (Exception e) {
            log.error("❌ Error recording initial transaction for accountNumber: {}",
                    event.getAccountNumber(), e);
        }
    }

    /**
     * Process a deposit transaction
     *
     * NOTE: This now works independently without querying BankAccountService database
     * The balance tracking is done through transaction history
     */
    @Transactional
    public TransactionResponse processDeposit(Integer accountNumber, Integer customerId,
                                              Double amount, String description,
                                              String referenceNumber) {
        // Validate amount
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        // Get current balance from last transaction
        Double currentBalance = getCurrentBalance(accountNumber);

        // Create transaction record
        TransactionEntity transaction = createTransaction(
                accountNumber,
                customerId,
                TransactionType.DEPOSIT,
                amount,
                currentBalance,
                currentBalance + amount,
                description,
                referenceNumber
        );

        log.info("✅ Processed deposit: transactionId={}, accountNumber={}, amount={}",
                transaction.getTransactionId(), accountNumber, amount);

        // Publish event
        publishTransactionCreatedEvent(transaction, customerId);

        return toResponse(transaction);
    }

    /**
     * Process a withdrawal transaction
     */
    @Transactional
    public TransactionResponse processWithdrawal(Integer accountNumber, Integer customerId,
                                                 Double amount, String description,
                                                 String referenceNumber) {
        // Validate amount
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        // Get current balance from last transaction
        Double currentBalance = getCurrentBalance(accountNumber);

        // Check sufficient funds
        if (currentBalance < amount) {
            throw new IllegalArgumentException(
                    "Insufficient funds. Available: " + currentBalance +
                            ", Requested: " + amount);
        }

        // Create transaction record
        TransactionEntity transaction = createTransaction(
                accountNumber,
                customerId,
                TransactionType.WITHDRAWAL,
                amount,
                currentBalance,
                currentBalance - amount,
                description,
                referenceNumber
        );

        log.info("✅ Processed withdrawal: transactionId={}, accountNumber={}, amount={}",
                transaction.getTransactionId(), accountNumber, amount);

        // Publish event
        publishTransactionCreatedEvent(transaction, customerId);

        return toResponse(transaction);
    }

    /**
     * Get current balance for an account from transaction history
     */
    private Double getCurrentBalance(Integer accountNumber) {
        return transactionRepository
                .findTopByAccountNumberOrderByTransactionDateDesc(accountNumber)
                .map(TransactionEntity::getBalanceAfter)
                .orElse(0.0);
    }

    /**
     * Get transaction history for an account
     */
    public List<TransactionResponse> getTransactionHistory(Integer accountNumber) {
        List<TransactionEntity> transactions = transactionRepository
                .findByAccountNumberOrderByTransactionDateDesc(accountNumber);

        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get transaction by transaction ID
     */
    public TransactionResponse getTransaction(String transactionId) {
        TransactionEntity transaction = transactionRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + transactionId));
        return toResponse(transaction);
    }

    /**
     * Get transactions by date range
     */
    public List<TransactionResponse> getTransactionsByDateRange(
            Integer accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        List<TransactionEntity> transactions = transactionRepository
                .findTransactionsByDateRange(accountNumber, startDate, endDate);

        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get account balance (calculated from transactions)
     */
    public Double getAccountBalance(Integer accountNumber) {
        return getCurrentBalance(accountNumber);
    }

    // ========== HELPER METHODS ==========

    /**
     * Create transaction record
     */
    private TransactionEntity createTransaction(
            Integer accountNumber,
            Integer customerId,
            TransactionType type,
            Double amount,
            Double balanceBefore,
            Double balanceAfter,
            String description,
            String referenceNumber) {

        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId(generateTransactionId());
        transaction.setAccountNumber(accountNumber);
        transaction.setCustomerId(customerId);  // Store for quick lookup
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setDescription(description);
        transaction.setReferenceNumber(referenceNumber);
        transaction.setTransactionDate(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = String.format("%04d", random.nextInt(10000));
        return "TXN-" + timestamp + "-" + randomPart;
    }

    /**
     * Publish transaction created event
     */
    private void publishTransactionCreatedEvent(TransactionEntity transaction, Integer customerId) {
        try {
            TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                    .transactionId(transaction.getTransactionId())
                    .accountNumber(transaction.getAccountNumber())
                    .customerId(customerId)
                    .transactionType(transaction.getTransactionType().name())
                    .amount(transaction.getAmount())
                    .balanceBefore(transaction.getBalanceBefore())
                    .balanceAfter(transaction.getBalanceAfter())
                    .status(transaction.getStatus().name())
                    .description(transaction.getDescription())
                    .referenceNumber(transaction.getReferenceNumber())
                    .transactionDate(transaction.getTransactionDate())
                    .eventId(UUID.randomUUID().toString())
                    .eventTimestamp(LocalDateTime.now())
                    .eventSource("transaction-service")
                    .build();

            kafkaTemplate.send("transaction-created",
                    transaction.getTransactionId(),
                    event);

            log.info("✅ Published TransactionCreatedEvent for transactionId: {}",
                    transaction.getTransactionId());

        } catch (Exception e) {
            log.error("❌ Failed to publish TransactionCreatedEvent for transactionId: {}",
                    transaction.getTransactionId(), e);
        }
    }

    /**
     * Convert entity to DTO
     */
    private TransactionResponse toResponse(TransactionEntity entity) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(entity.getTransactionId());
        response.setAccountNumber(entity.getAccountNumber());
        response.setTransactionType(entity.getTransactionType());
        response.setAmount(entity.getAmount());
        response.setBalanceBefore(entity.getBalanceBefore());
        response.setBalanceAfter(entity.getBalanceAfter());
        response.setStatus(entity.getStatus());
        response.setDescription(entity.getDescription());
        response.setTransactionDate(entity.getTransactionDate());
        response.setReferenceNumber(entity.getReferenceNumber());
        return response;
    }
}