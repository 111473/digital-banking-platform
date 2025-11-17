package com.banking.accountopening.service;

import com.banking.accountopening.dto.AccountOpeningRequest;
import com.banking.accountopening.dto.AccountOpeningResponse;
import com.banking.accountopening.entity.AccountOpeningEntity;
import com.banking.accountopening.enums.ApplicationStatus;
import com.banking.accountopening.enums.KYCStatus;
import com.banking.accountopening.exception.*;
import com.banking.accountopening.repository.AccountOpeningRepository;
import com.banking.events.ApplicationApprovedEvent;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AccountOpeningService {

    private final AccountOpeningRepository repository;
    private final EntityManager entityManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AccountOpeningService(AccountOpeningRepository repository,
                                 EntityManager entityManager,
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public AccountOpeningResponse applyForAccount(AccountOpeningRequest request) {
        AccountOpeningEntity entity = new AccountOpeningEntity();

        Integer nextId = ((Number) entityManager
                .createNativeQuery("SELECT nextval('application_id_sequence')")
                .getSingleResult())
                .intValue();

        entity.setApplicationId(nextId);
        entity.setAccountType(request.getAccountType());
        entity.setCurrencyType(request.getCurrencyType());
        entity.setFirstName(request.getFirstName());
        entity.setMiddleName(request.getMiddleName());
        entity.setLastName(request.getLastName());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setEmail(request.getEmail());
        entity.setRegion(request.getRegion());
        entity.setProvince(request.getProvince());
        entity.setMunicipality(request.getMunicipality());
        entity.setStreet(request.getStreet());
        entity.setIdentityType(request.getIdentityType());
        entity.setIdRefNumber(request.getIdRefNumber());
        entity.setApplicationDate(LocalDateTime.now());
        entity.setApplicationStatus(ApplicationStatus.PENDING);
        entity.setKycStatus(KYCStatus.PENDING);

        AccountOpeningEntity saved = repository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public AccountOpeningResponse submitApplication(Integer applicationId) {
        AccountOpeningEntity entity = repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with ID: " + applicationId));

        if (entity.getApplicationStatus() != ApplicationStatus.PENDING) {
            throw new ApplicationPendingException(
                    "Only pending applications can be submitted. Current status: " +
                            entity.getApplicationStatus());
        }

        entity.setApplicationStatus(ApplicationStatus.SUBMITTED);
        AccountOpeningEntity updated = repository.save(entity);
        return toResponse(updated);
    }

    @Transactional
    public AccountOpeningResponse startReview(Integer applicationId) {
        AccountOpeningEntity entity = repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with ID: " + applicationId));

        if (entity.getApplicationStatus() != ApplicationStatus.SUBMITTED) {
            throw new ApplicationNotSubmittedException(
                    "Cannot start review: application must be SUBMITTED first. " +
                            "Current status: " + entity.getApplicationStatus());
        }

        entity.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
        AccountOpeningEntity updated = repository.save(entity);
        return toResponse(updated);
    }

    @Transactional
    public AccountOpeningResponse updateKycStatus(Integer applicationId, KYCStatus kycStatus) {
        AccountOpeningEntity entity = repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with ID: " + applicationId));

        entity.setKycStatus(kycStatus);
        AccountOpeningEntity updated = repository.save(entity);
        return toResponse(updated);
    }

    /**
     * ✅ UPDATED: Now publishes Kafka event after approval
     */
    @Transactional
    public AccountOpeningResponse approveApplication(Integer applicationId) {
        AccountOpeningEntity entity = repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with ID: " + applicationId));

        if (entity.getApplicationStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationApprovalException(
                    "Cannot approve application: status must be UNDER_REVIEW. " +
                            "Current status: " + entity.getApplicationStatus());
        }

        if (entity.getKycStatus() != KYCStatus.VERIFIED) {
            throw new KYCVerificationException(
                    "Cannot approve application: KYC must be VERIFIED. " +
                            "Current KYC status: " + entity.getKycStatus());
        }

        entity.setApplicationStatus(ApplicationStatus.APPROVED);
        AccountOpeningEntity updated = repository.save(entity);

        // ✅ PUBLISH EVENT TO KAFKA
        publishApplicationApprovedEvent(updated);

        return toResponse(updated);
    }

    /**
     * ✅ NEW METHOD: Publishes event to Kafka
     * This triggers customer account creation in CustomerAccountService
     */
    private void publishApplicationApprovedEvent(AccountOpeningEntity application) {
        try {
            ApplicationApprovedEvent event = ApplicationApprovedEvent.builder()
                    .applicationId(application.getApplicationId())
                    .firstName(application.getFirstName())
                    .middleName(application.getMiddleName())
                    .lastName(application.getMiddleName())
                    .email(application.getEmail())
                    .phoneNumber(application.getPhoneNumber())
                    .region(application.getRegion())
                    .province(application.getProvince())
                    .municipality(application.getMunicipality())
                    .street(application.getStreet())
                    .identityType(application.getIdentityType().name())
                    .idRefNumber(application.getIdRefNumber())
                    .accountType(application.getAccountType().name())
                    .currencyType(application.getCurrencyType().name())
                    .kycStatus(application.getKycStatus().name())
                    .applicationDate(application.getApplicationDate())
                    .eventId(UUID.randomUUID().toString())
                    .eventTimestamp(LocalDateTime.now())
                    .eventSource("account-opening-service")
                    .build();

            kafkaTemplate.send("application-approved",
                    application.getApplicationId().toString(),
                    event);

            log.info("✅ Published ApplicationApprovedEvent for applicationId: {}",
                    application.getApplicationId());

        } catch (Exception e) {
            log.error("❌ Failed to publish ApplicationApprovedEvent for applicationId: {}",
                    application.getApplicationId(), e);
            // Note: Application is still approved in database even if event fails
            // Consider implementing event sourcing or outbox pattern for production
        }
    }

    @Transactional
    public AccountOpeningResponse rejectApplication(Integer applicationId) {
        AccountOpeningEntity entity = repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with ID: " + applicationId));

        if (entity.getApplicationStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new ApplicationRejectionException(
                    "Cannot reject application: status must be UNDER_REVIEW. " +
                            "Current status: " + entity.getApplicationStatus());
        }

        if (entity.getKycStatus() != KYCStatus.REJECTED) {
            throw new KYCVerificationException(
                    "Cannot reject application: KYC must be REJECTED before rejection is allowed. " +
                            "Current KYC status: " + entity.getKycStatus());
        }

        entity.setApplicationStatus(ApplicationStatus.REJECTED);
        AccountOpeningEntity updated = repository.save(entity);
        return toResponse(updated);
    }

    // Query methods remain unchanged
    public AccountOpeningResponse getApplication(Integer applicationId) {
        AccountOpeningEntity entity = repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with ID: " + applicationId));
        return toResponse(entity);
    }

    public List<AccountOpeningResponse> getAllApplications() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AccountOpeningResponse> getApplicationsByStatus(ApplicationStatus status) {
        return repository.findByApplicationStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AccountOpeningResponse toResponse(AccountOpeningEntity entity) {
        AccountOpeningResponse response = new AccountOpeningResponse();
        response.setApplicationId(entity.getApplicationId());
        response.setAccountType(entity.getAccountType());
        response.setCurrencyType(entity.getCurrencyType());
        response.setApplicationStatus(entity.getApplicationStatus());
        response.setKycStatus(entity.getKycStatus());
        response.setFirstName(entity.getFirstName());
        response.setMiddleName(entity.getMiddleName());
        response.setLastName(entity.getLastName());
        response.setEmail(entity.getEmail());
        response.setApplicationDate(entity.getApplicationDate());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}