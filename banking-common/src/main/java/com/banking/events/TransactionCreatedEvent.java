package com.banking.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a transaction is completed
 * This can trigger notifications, fraud detection, reporting, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreatedEvent {

    // Transaction details
    private String transactionId;
    private Integer accountNumber;
    private Integer customerId;
    private String transactionType;  // DEPOSIT, WITHDRAWAL, TRANSFER
    private Double amount;
    private Double balanceBefore;
    private Double balanceAfter;
    private String status;
    private String description;
    private String referenceNumber;
    private LocalDateTime transactionDate;

    // Event metadata
    private String eventId;
    private LocalDateTime eventTimestamp;
    private String eventSource;   // "transaction-service"
}