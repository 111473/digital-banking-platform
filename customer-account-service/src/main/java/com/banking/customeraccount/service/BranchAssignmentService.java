package com.banking.customeraccount.service;

import com.banking.customeraccount.client.BranchServiceClient;
import com.banking.customeraccount.dto.BranchInfoDTO;
import com.banking.customeraccount.entity.CustomerAccountEntity;
import com.banking.events.BranchAssignmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for assigning customers to branches
 * Uses intelligent assignment algorithms based on various factors
 */
@Slf4j
@Service
public class BranchAssignmentService {

    private final BranchServiceClient branchClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Default branches for assignment (configurable)
    private static final List<String> DEFAULT_BRANCH_CODES = Arrays.asList(
            "BR001", "BR002", "BR003", "BR004", "BR005"
    );

    public BranchAssignmentService(
            BranchServiceClient branchClient,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.branchClient = branchClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Assign customer to the best available branch
     * Algorithm: Round-robin or location-based (simplified for now)
     */
    public String assignBranchToCustomer(CustomerAccountEntity customer) {
        log.info("Starting branch assignment for customer: {}", customer.getCustomerId());

        // Try to find an active branch
        for (String branchCode : DEFAULT_BRANCH_CODES) {
            if (branchClient.isBranchActive(branchCode)) {
                log.info("Assigned customer {} to branch {}",
                        customer.getCustomerId(), branchCode);

                // Publish branch assignment event
                publishBranchAssignmentEvent(customer, branchCode, "AUTO_ASSIGNMENT");

                return branchCode;
            }
        }

        // Fallback: Assign to first branch if none are verified as active
        String fallbackBranch = DEFAULT_BRANCH_CODES.get(0);
        log.warn("No active branches verified. Assigning customer {} to fallback branch {}",
                customer.getCustomerId(), fallbackBranch);

        publishBranchAssignmentEvent(customer, fallbackBranch, "FALLBACK_ASSIGNMENT");

        return fallbackBranch;
    }

    /**
     * Reassign customer to a different branch
     */
    public String reassignCustomerBranch(CustomerAccountEntity customer, String newBranchCode) {
        log.info("Reassigning customer {} from branch {} to {}",
                customer.getCustomerId(), customer.getBranchCode(), newBranchCode);

        // Validate new branch
        if (!branchClient.isValidBranchCodeFormat(newBranchCode)) {
            throw new IllegalArgumentException("Invalid branch code format: " + newBranchCode);
        }

        BranchInfoDTO branchInfo = branchClient.getBranchInfo(newBranchCode);
        if (branchInfo == null) {
            throw new IllegalArgumentException("Branch not found: " + newBranchCode);
        }

        if (!"ACTIVE".equalsIgnoreCase(branchInfo.getStatus())) {
            throw new IllegalArgumentException("Branch is not active: " + newBranchCode);
        }

        // Publish reassignment event
        publishBranchAssignmentEvent(customer, newBranchCode, "MANUAL_REASSIGNMENT");

        return newBranchCode;
    }

    /**
     * Get branch information for a customer
     */
    public BranchInfoDTO getBranchInfoForCustomer(CustomerAccountEntity customer) {
        if (customer.getBranchCode() == null) {
            return null;
        }
        return branchClient.getBranchInfo(customer.getBranchCode());
    }

    /**
     * Publish branch assignment event to Kafka
     */
    private void publishBranchAssignmentEvent(
            CustomerAccountEntity customer,
            String branchCode,
            String reason) {

        try {
            BranchAssignmentEvent event = BranchAssignmentEvent.builder()
                    .customerId(customer.getCustomerId())
                    .applicationId(customer.getApplicationId())
                    .branchCode(branchCode)
                    .assignmentReason(reason)
                    .eventId(UUID.randomUUID().toString())
                    .eventTimestamp(LocalDateTime.now())
                    .eventSource("customer-account-service")
                    .build();

            kafkaTemplate.send("branch-assignment-events", event);
            log.info("Published branch assignment event for customer {}", customer.getCustomerId());

        } catch (Exception e) {
            log.error("Failed to publish branch assignment event: {}", e.getMessage());
            // Don't throw - assignment should succeed even if event publishing fails
        }
    }
}