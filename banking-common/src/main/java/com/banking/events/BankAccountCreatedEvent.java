package com.banking.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when bank account is created
 * This can trigger transaction history initialization, notifications, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountCreatedEvent {

    // Bank account details
    private Integer accountNumber;
    private Integer customerId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String branchCode;
    private String accountType;
    private Double initialBalance;
    private Double interestRate;
    private String accountStatus;

    // Event metadata
    private String eventId;
    private LocalDateTime eventTimestamp;
    private String eventSource;   // "bank-account-service"
}