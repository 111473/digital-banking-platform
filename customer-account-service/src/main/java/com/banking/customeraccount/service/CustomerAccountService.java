package com.banking.customeraccount.service;

import com.banking.customeraccount.dto.BranchInfoDTO;
import com.banking.customeraccount.dto.CustomerAccountResponse;
import com.banking.customeraccount.entity.CustomerAccountEntity;
import com.banking.customeraccount.enums.AccountType;
import com.banking.customeraccount.enums.CurrencyType;
import com.banking.customeraccount.enums.IdentityType;
import com.banking.customeraccount.enums.KYCStatus;
import com.banking.customeraccount.exception.ResourceNotFoundException;
import com.banking.customeraccount.repository.CustomerAccountRepository;
import com.banking.events.ApplicationApprovedEvent;
import com.banking.events.CustomerAccountCreatedEvent;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Customer Account Service
 *
 * Responsibilities:
 * - Listen to application-approved events
 * - Create customer accounts with automatic branch assignment
 * - Publish customer-account-created events
 * - Manage customer account lifecycle
 *
 * Event Flow:
 * IN:  ApplicationApprovedEvent (from account-opening-service)
 * OUT: CustomerAccountCreatedEvent (to bank-account-service)
 * OUT: BranchAssignmentEvent (to branch-service)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAccountService {

    private final CustomerAccountRepository customerRepository;
    private final EntityManager entityManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BranchAssignmentService branchAssignmentService;

    /**
     * Kafka Event Listener - Application Approved
     *
     * Triggered when: Account Opening Service approves an application
     * Action: Create customer account, assign branch, and publish event
     * Idempotency: Checks if customer already exists before creating
     * Error Handling: Retries 3 times, then logged for manual intervention
     *
     * @param event ApplicationApprovedEvent from account-opening-service
     * @param partition Kafka partition for logging
     * @param offset Kafka offset for logging/debugging
     */
    @KafkaListener(
            topics = "application-approved",
            groupId = "customer-account-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void handleApplicationApproved(
            @Payload ApplicationApprovedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("📨 [RECEIVED] ApplicationApprovedEvent | ApplicationId: {} | Partition: {} | Offset: {}",
                event.getApplicationId(), partition, offset);

        try {
            // Step 1: Idempotency check
            Optional<CustomerAccountEntity> existing =
                    customerRepository.findByApplicationId(event.getApplicationId());

            if (existing.isPresent()) {
                log.warn("⚠️ [DUPLICATE] Customer account already exists | ApplicationId: {} | CustomerId: {} | Skipping",
                        event.getApplicationId(), existing.get().getCustomerId());
                return;
            }

            // Step 2: Generate customer ID from database sequence
            Integer customerId = ((Number) entityManager
                    .createNativeQuery("SELECT nextval('customer_id_sequence')")
                    .getSingleResult())
                    .intValue();

            // Step 3: Create customer account entity
            CustomerAccountEntity customer = buildCustomerAccount(event, customerId);

            // Step 4: Save to database (without branch first)
            CustomerAccountEntity savedCustomer = customerRepository.save(customer);

            log.info("✅ [CREATED] Customer account | CustomerId: {} | ApplicationId: {} | Name: {} | Type: {}",
                    savedCustomer.getCustomerId(),
                    savedCustomer.getApplicationId(),
                    savedCustomer.getFirstName(),
                    savedCustomer.getMiddleName(),
                    savedCustomer.getLastName(),
                    savedCustomer.getAccountType());

            // Step 5: Automatic Branch Assignment (NEW)
            try {
                String assignedBranch = branchAssignmentService.assignBranchToCustomer(savedCustomer);
                savedCustomer.setBranchCode(assignedBranch);
                savedCustomer = customerRepository.save(savedCustomer);

                log.info("🏦 [BRANCH_ASSIGNED] Customer {} assigned to branch {}",
                        savedCustomer.getCustomerId(), assignedBranch);
            } catch (Exception e) {
                log.error("❌ [BRANCH_ASSIGNMENT_ERROR] Failed to assign branch to customer {}: {}",
                        savedCustomer.getCustomerId(), e.getMessage());
                // Customer is still created, just without branch assignment
            }

            // Step 6: Publish event to downstream services
            publishCustomerAccountCreatedEvent(savedCustomer);

        } catch (IllegalArgumentException e) {
            log.error("❌ [VALIDATION_ERROR] Invalid data in event | ApplicationId: {} | Error: {}",
                    event.getApplicationId(), e.getMessage());
            throw e; // Will trigger retry

        } catch (Exception e) {
            log.error("❌ [PROCESSING_ERROR] Failed to create customer account | ApplicationId: {} | Error: {}",
                    event.getApplicationId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create customer account", e); // Will trigger retry
        }
    }

    /**
     * Build customer account entity from event data
     *
     * @param event ApplicationApprovedEvent
     * @param customerId Generated customer ID
     * @return CustomerAccountEntity ready to be saved
     */
    private CustomerAccountEntity buildCustomerAccount(ApplicationApprovedEvent event, Integer customerId) {
        // Convert string enums to enum types (with validation)
        IdentityType identityType;
        AccountType accountType;
        CurrencyType currencyType;
        KYCStatus kycStatus;

        try {
            identityType = IdentityType.valueOf(event.getIdentityType());
            accountType = AccountType.valueOf(event.getAccountType());
            currencyType = CurrencyType.valueOf(event.getCurrencyType());
            kycStatus = KYCStatus.valueOf(event.getKycStatus());
        } catch (IllegalArgumentException e) {
            log.error("❌ [ENUM_ERROR] Invalid enum value | ApplicationId: {} | Error: {}",
                    event.getApplicationId(), e.getMessage());
            throw new IllegalArgumentException("Invalid enum value in event: " + e.getMessage());
        }

        // Build entity using builder pattern with converted enums
        return CustomerAccountEntity.builder()
                .customerId(customerId)
                .applicationId(event.getApplicationId())
                .firstName(event.getFirstName())
                .middleName(event.getMiddleName())
                .lastName(event.getLastName())
                .email(event.getEmail())
                .phoneNumber(event.getPhoneNumber())
                .region(event.getRegion())
                .province(event.getProvince())
                .municipality(event.getMunicipality())
                .street(event.getStreet())
                .identityType(identityType)
                .idRefNumber(event.getIdRefNumber())
                .accountType(accountType)
                .currencyType(currencyType)
                .kycStatus(kycStatus)
                .applicationDate(event.getApplicationDate())
                .build();
    }

    /**
     * Publish Customer Account Created Event
     *
     * Purpose: Notify bank-account-service to create bank account
     * Topic: customer-account-created
     * Key: customerId (for partitioning)
     * UPDATED: Now includes branch information
     *
     * @param customer Saved customer account entity
     */
    private void publishCustomerAccountCreatedEvent(CustomerAccountEntity customer) {
        try {
            CustomerAccountCreatedEvent event = CustomerAccountCreatedEvent.builder()
                    .customerId(customer.getCustomerId())
                    .applicationId(customer.getApplicationId())
                    .firstName(customer.getFirstName())
                    .middleName(customer.getMiddleName())
                    .lastName(customer.getLastName())
                    .email(customer.getEmail())
                    .phoneNumber(customer.getPhoneNumber())
                    .accountType(customer.getAccountType().name())
                    .currencyType(customer.getCurrencyType().name())
                    .branchCode(customer.getBranchCode())  // NEW: Include branch code
                    .kycStatus(customer.getKycStatus().name())
                    .kycVerifiedDate(customer.getKycVerifiedDate())
                    .eventId(UUID.randomUUID().toString())
                    .eventTimestamp(LocalDateTime.now())
                    .eventSource("customer-account-service")
                    .build();

            // Send to Kafka with customer ID as key (for partitioning)
            kafkaTemplate.send("customer-account-created",
                    customer.getCustomerId().toString(),
                    event);

            log.info("📤 [PUBLISHED] CustomerAccountCreatedEvent | CustomerId: {} | ApplicationId: {} | Branch: {} | EventId: {}",
                    customer.getCustomerId(),
                    customer.getApplicationId(),
                    customer.getBranchCode(),
                    event.getEventId());

        } catch (Exception e) {
            log.error("❌ [PUBLISH_ERROR] Failed to publish CustomerAccountCreatedEvent | CustomerId: {} | Error: {}",
                    customer.getCustomerId(), e.getMessage(), e);
            // Don't throw - customer is already saved, event publish failure shouldn't rollback
            // In production: Store failed events in outbox table for retry
        }
    }

    // ========== QUERY METHODS ==========

    /**
     * Get customer by customer ID
     * UPDATED: Now includes branch information
     */
    @Transactional(readOnly = true)
    public CustomerAccountResponse getCustomerByCustomerId(Integer customerId) {
        CustomerAccountEntity entity = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));
        return toResponse(entity);
    }

    /**
     * Get customer by application ID
     * UPDATED: Now includes branch information
     */
    @Transactional(readOnly = true)
    public CustomerAccountResponse getCustomerByApplicationId(Integer applicationId) {
        CustomerAccountEntity entity = customerRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found for application ID: " + applicationId));
        return toResponse(entity);
    }

    /**
     * Get all customers
     * UPDATED: Now includes branch information
     */
    @Transactional(readOnly = true)
    public List<CustomerAccountResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get customers by branch code (NEW)
     */
    @Transactional(readOnly = true)
    public List<CustomerAccountResponse> getCustomersByBranch(String branchCode) {
        return customerRepository.findByBranchCode(branchCode).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update customer contact information
     */
    @Transactional
    public CustomerAccountResponse updateCustomerContact(Integer customerId,
                                                         String phoneNumber,
                                                         String email,
                                                         String region,
                                                         String province,
                                                         String municipality,
                                                         String street) {
        CustomerAccountEntity customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));

        if (phoneNumber != null && !phoneNumber.isBlank()) {
            customer.setPhoneNumber(phoneNumber);
        }
        if (email != null && !email.isBlank()) {
            customer.setEmail(email);
        }
        if (region != null && !region.isBlank()) {
            customer.setRegion(region);
        }
        if (province != null && !province.isBlank()) {
            customer.setProvince(province);
        }
        if (municipality != null && !municipality.isBlank()) {
            customer.setMunicipality(municipality);
        }
        if (street != null && !street.isBlank()) {
            customer.setStreet(street);
        }

        CustomerAccountEntity updated = customerRepository.save(customer);
        log.info("✅ [CONTACT_UPDATED] CustomerId: {} | Phone: {} | Email: {} | Region: {} | Province: {} | Municipality: {} | Street: {}",
                customerId, phoneNumber != null, email != null, region != null, province != null, municipality != null, street != null);

        return toResponse(updated);
    }

    /**
     * Update customer branch assignment (NEW)
     */
    @Transactional
    public CustomerAccountResponse updateCustomerBranch(Integer customerId,
                                                        String newBranchCode) {
        CustomerAccountEntity customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));

        // Validate and assign new branch
        String assignedBranch = branchAssignmentService.reassignCustomerBranch(
                customer, newBranchCode);

        customer.setBranchCode(assignedBranch);
        CustomerAccountEntity updated = customerRepository.save(customer);

        log.info("✅ [BRANCH_REASSIGNED] CustomerId: {} | OldBranch: {} | NewBranch: {}",
                customerId, customer.getBranchCode(), assignedBranch);

        return toResponse(updated);
    }

    /**
     * Update KYC status
     */
    @Transactional
    public CustomerAccountResponse updateKycStatus(Integer customerId,
                                                   KYCStatus status,
                                                   String verifiedBy) {
        CustomerAccountEntity customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with ID: " + customerId));

        customer.setKycStatus(status);
        if (status == KYCStatus.VERIFIED) {
            customer.setKycVerifiedDate(LocalDateTime.now());
        }

        CustomerAccountEntity updated = customerRepository.save(customer);

        log.info("✅ [KYC_UPDATED] CustomerId: {} | Status: {} | VerifiedBy: {}",
                customerId, status, verifiedBy);

        return toResponse(updated);
    }

    /**
     * Check if customer exists for an application
     */
    public boolean customerAccountExists(Integer applicationId) {
        return customerRepository.findByApplicationId(applicationId).isPresent();
    }

    /**
     * Map entity to response DTO
     * UPDATED: Now includes branch information
     */
    private CustomerAccountResponse toResponse(CustomerAccountEntity entity) {
        // Fetch branch information if branch code exists
        BranchInfoDTO branchInfo = null;
        if (entity.getBranchCode() != null) {
            branchInfo = branchAssignmentService.getBranchInfoForCustomer(entity);
        }

        CustomerAccountResponse response = new CustomerAccountResponse();
        response.setCustomerId(entity.getCustomerId());
        response.setApplicationId(entity.getApplicationId());
        response.setFirstName(entity.getFirstName());
        response.setMiddleName(entity.getMiddleName());
        response.setLastName(entity.getLastName());
        response.setEmail(entity.getEmail());
        response.setPhoneNumber(entity.getPhoneNumber());
        response.setAccountType(entity.getAccountType());
        response.setCurrencyType(entity.getCurrencyType());
        response.setKycStatus(entity.getKycStatus());
        response.setCreatedAt(entity.getCreatedAt());

        // NEW: Branch information
        response.setBranchCode(entity.getBranchCode());
        response.setBranchInfo(branchInfo);

        return response;
    }
}