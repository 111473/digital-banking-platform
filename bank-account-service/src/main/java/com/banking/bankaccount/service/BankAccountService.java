package com.banking.bankaccount.service;

import com.banking.bankaccount.dto.BankAccountResponse;
import com.banking.bankaccount.entity.BankAccountEntity;
import com.banking.bankaccount.enums.AccountStatus;
import com.banking.bankaccount.enums.AccountType;
import com.banking.bankaccount.exception.ResourceNotFoundException;
import com.banking.bankaccount.repository.BankAccountRepository;
import com.banking.events.BankAccountCreatedEvent;
import com.banking.events.CustomerAccountCreatedEvent;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bank Account Service
 *
 * Responsibilities:
 * - Listen to customer-account-created events
 * - Create bank accounts with branch assignment
 * - Publish bank-account-created events
 * - Manage bank account operations (deposit, withdraw, etc.)
 */
@Slf4j
@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final EntityManager entityManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BankAccountService(BankAccountRepository bankAccountRepository,
                              EntityManager entityManager,
                              KafkaTemplate<String, Object> kafkaTemplate) {
        this.bankAccountRepository = bankAccountRepository;
        this.entityManager = entityManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Kafka Event Listener - Customer Account Created
     *
     * Triggered when: Customer Account Service creates a customer account
     * Action: Create bank account with branch assignment and publish event
     * Idempotency: Checks if bank account already exists before creating
     * Error Handling: Retries on failure, with manual acknowledgment
     */
    @KafkaListener(
            topics = "customer-account-created",
            groupId = "bank-account-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleCustomerAccountCreated(
            @Payload CustomerAccountCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("📨 [RECEIVED] CustomerAccountCreatedEvent | CustomerId: {} | ApplicationId: {} | BranchCode: {} | Partition: {} | Offset: {}",
                event.getCustomerId(), event.getApplicationId(), event.getBranchCode(), partition, offset);

        try {
            // Step 1: Idempotency check
            List<BankAccountEntity> existingAccounts =
                    bankAccountRepository.findByCustomerId(event.getCustomerId());

            if (!existingAccounts.isEmpty()) {
                log.warn("⚠️ [DUPLICATE] Bank account already exists | CustomerId: {} | AccountCount: {} | Skipping",
                        event.getCustomerId(), existingAccounts.size());
                acknowledgment.acknowledge();  // ACKNOWLEDGE DUPLICATE
                return;
            }

            // Step 2: Generate account number from database sequence
            Integer accountNumber = ((Number) entityManager
                    .createNativeQuery("SELECT nextval('account_number_sequence')")
                    .getSingleResult())
                    .intValue();

            // Step 3: Parse account type safely
            AccountType accountType;
            try {
                accountType = AccountType.valueOf(event.getAccountType());
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ [ENUM_ERROR] Unknown account type: {}, defaulting to SAVINGS",
                        event.getAccountType());
                accountType = AccountType.SAVINGS;
            }

            // Step 4: Create bank account entity
            BankAccountEntity bankAccount = new BankAccountEntity();
            bankAccount.setAccountNumber(accountNumber);
            bankAccount.setCustomerId(event.getCustomerId());
            bankAccount.setCustomerName(event.getName());
            bankAccount.setAccountType(accountType);

            // ✅ NEW: Set branch information
            bankAccount.setBranchCode(event.getBranchCode());

            // Set initial balance based on account type
            Double initialDeposit = getDefaultInitialDeposit(event.getAccountType());
            bankAccount.setBalance(initialDeposit);

            // Calculate interest rate
            bankAccount.setInterestRate(calculateInterestRate(accountType));
            bankAccount.setAccountStatus(AccountStatus.ACTIVE);

            // Step 5: Save to database
            BankAccountEntity saved = bankAccountRepository.save(bankAccount);

            log.info("✅ [CREATED] Bank account | AccountNumber: {} | CustomerId: {} | ApplicationId: {} | BranchCode: {} | Type: {} | Balance: {}",
                    saved.getAccountNumber(),
                    saved.getCustomerId(),
                    event.getApplicationId(),
                    saved.getBranchCode(),
                    saved.getAccountType(),
                    saved.getBalance());

            // Step 6: Publish event to downstream services
            publishBankAccountCreatedEvent(saved);

            // Step 7: ACKNOWLEDGE SUCCESS
            acknowledgment.acknowledge();

            log.info("✅ [ACKNOWLEDGED] Message processed | CustomerId: {} | AccountNumber: {} | BranchCode: {} | Offset: {}",
                    event.getCustomerId(), saved.getAccountNumber(), saved.getBranchCode(), offset);

        } catch (IllegalArgumentException e) {
            log.error("❌ [VALIDATION_ERROR] Invalid data in event | CustomerId: {} | ApplicationId: {} | Error: {}",
                    event.getCustomerId(), event.getApplicationId(), e.getMessage());
            // DON'T acknowledge - will retry
            throw e;

        } catch (Exception e) {
            log.error("❌ [PROCESSING_ERROR] Failed to create bank account | CustomerId: {} | ApplicationId: {} | Error: {}",
                    event.getCustomerId(), event.getApplicationId(), e.getMessage(), e);
            // DON'T acknowledge - will retry
            throw new RuntimeException("Failed to create bank account", e);
        }
    }

    /**
     * Publish Bank Account Created Event
     *
     * Purpose: Notify other services about new bank account
     * Topic: bank-account-created
     * Key: accountNumber (for partitioning)
     */
    private void publishBankAccountCreatedEvent(BankAccountEntity bankAccount) {
        try {
            BankAccountCreatedEvent event = BankAccountCreatedEvent.builder()
                    .accountNumber(bankAccount.getAccountNumber())
                    .customerId(bankAccount.getCustomerId())
                    .customerName(bankAccount.getCustomerName())
                    .branchCode(bankAccount.getBranchCode())  // ✅ NEW
                    .accountType(bankAccount.getAccountType().name())
                    .initialBalance(bankAccount.getBalance())
                    .interestRate(bankAccount.getInterestRate())
                    .accountStatus(bankAccount.getAccountStatus().name())
                    .eventId(UUID.randomUUID().toString())
                    .eventTimestamp(LocalDateTime.now())
                    .eventSource("bank-account-service")
                    .build();

            kafkaTemplate.send("bank-account-created",
                    bankAccount.getAccountNumber().toString(),
                    event);

            log.info("📤 [PUBLISHED] BankAccountCreatedEvent | AccountNumber: {} | CustomerId: {} | EventId: {}",
                    bankAccount.getAccountNumber(),
                    bankAccount.getCustomerId(),
                    event.getEventId());

        } catch (Exception e) {
            log.error("❌ [PUBLISH_ERROR] Failed to publish BankAccountCreatedEvent | AccountNumber: {} | Error: {}",
                    bankAccount.getAccountNumber(), e.getMessage(), e);
            // Don't throw - bank account is already saved, event publish failure shouldn't rollback
        }
    }

    // ========== QUERY METHODS ==========

    public BankAccountResponse getBankAccount(Integer accountNumber) {
        BankAccountEntity account = bankAccountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bank account not found with account number: " + accountNumber));
        return toResponse(account);
    }

    public List<BankAccountResponse> getAccountsByCustomerId(Integer customerId) {
        return bankAccountRepository.findByCustomerId(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BankAccountResponse> getAllBankAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ✅ NEW: Get accounts by branch code
    public List<BankAccountResponse> getAccountsByBranchCode(String branchCode) {
        return bankAccountRepository.findByBranchCode(branchCode).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BankAccountResponse updateAccountStatus(Integer accountNumber, AccountStatus newStatus) {
        BankAccountEntity account = bankAccountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bank account not found with account number: " + accountNumber));

        account.setAccountStatus(newStatus);
        BankAccountEntity updated = bankAccountRepository.save(account);
        return toResponse(updated);
    }

    @Transactional
    public BankAccountResponse deposit(Integer accountNumber, Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        BankAccountEntity account = bankAccountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bank account not found with account number: " + accountNumber));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account must be ACTIVE to accept deposits");
        }

        account.setBalance(account.getBalance() + amount);
        BankAccountEntity updated = bankAccountRepository.save(account);
        return toResponse(updated);
    }

    @Transactional
    public BankAccountResponse withdraw(Integer accountNumber, Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        BankAccountEntity account = bankAccountRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bank account not found with account number: " + accountNumber));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account must be ACTIVE for withdrawals");
        }

        if (account.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.setBalance(account.getBalance() - amount);
        BankAccountEntity updated = bankAccountRepository.save(account);
        return toResponse(updated);
    }

    // ========== HELPER METHODS ==========

    private Double getDefaultInitialDeposit(String accountType) {
        try {
            AccountType type = AccountType.valueOf(accountType);
            switch (type) {
                case TIME_DEPOSIT:
                    return 5000.0;  // Minimum for time deposit
                case SAVINGS:
                    return 1000.0;  // Minimum for savings
                default:
                    return 0.0;     // No minimum for current/join accounts
            }
        } catch (IllegalArgumentException e) {
            return 0.0;  // Default to 0 if unknown type
        }
    }

    private double calculateInterestRate(AccountType accountType) {
        switch (accountType) {
            case CURRENT:
                return 0.0;
            case SAVINGS:
                return 3.5;
            case JOIN_ACCOUNT:
                return 5.0;
            case TIME_DEPOSIT:
                return 7.0;
            default:
                return 0.0;
        }
    }

    private BankAccountResponse toResponse(BankAccountEntity entity) {
        BankAccountResponse response = new BankAccountResponse();
        response.setAccountNumber(entity.getAccountNumber());
        response.setCustomerId(entity.getCustomerId());
        response.setCustomerName(entity.getCustomerName());
        response.setBranchCode(entity.getBranchCode());  // ✅ NEW
        response.setAccountType(entity.getAccountType());
        response.setBalance(entity.getBalance());
        response.setInterestRate(entity.getInterestRate());
        response.setAccountStatus(entity.getAccountStatus());
        return response;
    }
}