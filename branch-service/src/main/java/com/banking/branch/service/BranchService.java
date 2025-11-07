package com.banking.branch.service;

import com.banking.branch.dto.BranchRequest;
import com.banking.branch.dto.BranchResponse;
import com.banking.branch.entity.BranchEntity;
import com.banking.branch.enums.BranchStatus;
import com.banking.branch.repository.BranchRepository;
import com.banking.events.BranchAssignmentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing branches
 * Listens to branch assignment events for analytics
 */
@Slf4j
@Service
public class BranchService {

    private final BranchRepository repository;

    // In-memory customer count (can be moved to database for persistence)
    private final Map<String, Long> branchCustomerCount = new HashMap<>();

    public BranchService(BranchRepository repository) {
        this.repository = repository;
    }

    /**
     * Create a new branch
     */
    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        log.info("Creating new branch: {}", request.getBranchCode());

        if (repository.existsByBranchCode(request.getBranchCode())) {
            throw new IllegalArgumentException(
                    "Branch already exists with code: " + request.getBranchCode());
        }

        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Branch already exists with email: " + request.getEmail());
        }

        BranchEntity branch = BranchEntity.builder()
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .region(request.getRegion())
                .province(request.getProvince())
                .city(request.getCity())
                .address(request.getAddress())
                .postalCode(request.getPostalCode())
                .managerName(request.getManagerName())
                .email(request.getEmail())
                .contactNumber(request.getContactNumber())
                .status(request.getStatus())
                .build();

        BranchEntity saved = repository.save(branch);
        log.info("Branch created successfully: {}", saved.getBranchCode());

        return mapToResponse(saved);
    }

    /**
     * Get branch by branch code
     */
    @Transactional(readOnly = true)
    public BranchResponse getBranchByCode(String branchCode) {
        BranchEntity branch = repository.findByBranchCode(branchCode)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchCode));

        return mapToResponse(branch);
    }

    /**
     * Get all branches
     */
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get branches by status
     */
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByStatus(BranchStatus status) {
        return repository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get branches by region
     */
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByRegion(String region) {
        return repository.findByRegion(region).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update branch information
     */
    @Transactional
    public BranchResponse updateBranch(String branchCode, BranchRequest request) {
        BranchEntity branch = repository.findByBranchCode(branchCode)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchCode));

        branch.setBranchName(request.getBranchName());
        branch.setRegion(request.getRegion());
        branch.setProvince(request.getProvince());
        branch.setCity(request.getCity());
        branch.setAddress(request.getAddress());
        branch.setPostalCode(request.getPostalCode());
        branch.setManagerName(request.getManagerName());
        branch.setContactNumber(request.getContactNumber());
        branch.setStatus(request.getStatus());

        // Only update email if it's different and not already used
        if (!branch.getEmail().equals(request.getEmail())) {
            if (repository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException(
                        "Email already in use: " + request.getEmail());
            }
            branch.setEmail(request.getEmail());
        }

        BranchEntity updated = repository.save(branch);
        log.info("Branch updated: {}", branchCode);

        return mapToResponse(updated);
    }

    /**
     * Update branch status
     */
    @Transactional
    public BranchResponse updateBranchStatus(String branchCode, BranchStatus status) {
        BranchEntity branch = repository.findByBranchCode(branchCode)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchCode));

        branch.setStatus(status);
        BranchEntity updated = repository.save(branch);

        log.info("Branch {} status updated to {}", branchCode, status);
        return mapToResponse(updated);
    }

    /**
     * Delete branch (soft delete by setting status to CLOSED)
     */
    @Transactional
    public void deleteBranch(String branchCode) {
        BranchEntity branch = repository.findByBranchCode(branchCode)
                .orElseThrow(() -> new RuntimeException("Branch not found: " + branchCode));

        branch.setStatus(BranchStatus.CLOSED);
        repository.save(branch);

        log.info("Branch closed: {}", branchCode);
    }

    /**
     * Get customer count for a branch
     */
    public Long getCustomerCount(String branchCode) {
        return branchCustomerCount.getOrDefault(branchCode, 0L);
    }

    /**
     * Listen to branch assignment events
     * This updates the customer count for analytics
     */
    @KafkaListener(
            topics = "branch-assignment-events",
            groupId = "branch-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleBranchAssignment(@Payload BranchAssignmentEvent event) {
        log.info("Received branch assignment event for customer {} to branch {}",
                event.getCustomerId(), event.getBranchCode());

        try {
            // Update customer count
            String branchCode = event.getBranchCode();
            Long currentCount = branchCustomerCount.getOrDefault(branchCode, 0L);
            branchCustomerCount.put(branchCode, currentCount + 1);

            log.info("Branch {} customer count updated to {}",
                    branchCode, branchCustomerCount.get(branchCode));

        } catch (Exception e) {
            log.error("Error processing branch assignment event: {}", e.getMessage());
        }
    }

    /**
     * Map entity to response DTO
     */
    private BranchResponse mapToResponse(BranchEntity entity) {
        return BranchResponse.builder()
                .branchCode(entity.getBranchCode())
                .branchName(entity.getBranchName())
                .region(entity.getRegion())
                .province(entity.getProvince())
                .city(entity.getCity())
                .address(entity.getAddress())
                .postalCode(entity.getPostalCode())
                .managerName(entity.getManagerName())
                .email(entity.getEmail())
                .contactNumber(entity.getContactNumber())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .totalCustomers(getCustomerCount(entity.getBranchCode()))
                .build();
    }
}