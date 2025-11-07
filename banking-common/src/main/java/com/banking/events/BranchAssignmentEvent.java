package com.banking.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a customer is assigned to a branch
 * Branch Service can listen to this event for analytics/tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchAssignmentEvent {

    private Integer customerId;
    private Integer applicationId;
    private String branchCode;
    private String assignmentReason;  // "AUTO_ASSIGNMENT", "MANUAL_ASSIGNMENT", etc.

    // Event metadata
    private String eventId;
    private LocalDateTime eventTimestamp;
    private String eventSource;   // "customer-account-service"
}