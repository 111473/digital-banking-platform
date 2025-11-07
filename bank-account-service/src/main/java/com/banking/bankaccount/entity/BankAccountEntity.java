package com.banking.bankaccount.entity;

import com.banking.bankaccount.enums.AccountStatus;
import com.banking.bankaccount.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bank Account Entity
 *
 * IMPORTANT: This entity does NOT directly reference CustomerAccountEntity
 * to maintain microservices independence. Instead, it stores customerId
 * as a foreign key reference.
 */
@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private Integer accountNumber;

    // ✅ Store customerId instead of CustomerAccountEntity reference
    // This maintains loose coupling between services
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    // Store customer info for quick access (denormalized)
    @Column(name = "customer_name", nullable = false)
    private String customerName;

    // ✅ NEW: Store branch information
    @Column(name = "branch_code")
    private String branchCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(name = "balance", nullable = false)
    private Double balance = 0.0;

    @Column(name = "interest_rate", nullable = false)
    private Double interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}