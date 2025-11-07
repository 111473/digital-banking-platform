package com.banking.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when an application is APPROVED
 * This replaces the direct database access between services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationApprovedEvent {

    // Application details
    private Integer applicationId;
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private String identityType;  // Store as String to avoid enum dependency
    private String idRefNumber;
    private String accountType;   // Store as String
    private String currencyType;  // Store as String
    private String kycStatus;     // Store as String
    private LocalDateTime applicationDate;

    // Event metadata
    private String eventId;
    private LocalDateTime eventTimestamp;
    private String eventSource;   // "account-opening-service"
}