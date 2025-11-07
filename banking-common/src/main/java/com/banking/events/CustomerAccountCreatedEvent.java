package com.banking.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when customer account is created
 * This can trigger bank account creation in BankAccountService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAccountCreatedEvent {

    // Customer details
    private Integer customerId;
    private Integer applicationId;
    private String name;
    private String email;
    private String phoneNumber;
    private String accountType;
    private String currencyType;

    // NEW: Branch information
    private String branchCode;

    // KYC information
    private String kycStatus;
    private LocalDateTime kycVerifiedDate;

    // Event metadata
    private String eventId;
    private LocalDateTime eventTimestamp;
    private String eventSource;   // "customer-account-service"
}