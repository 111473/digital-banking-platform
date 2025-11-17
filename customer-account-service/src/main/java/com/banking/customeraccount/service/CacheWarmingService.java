package com.banking.customeraccount.service;

import com.banking.customeraccount.entity.CustomerAccountEntity;
import com.banking.customeraccount.repository.CustomerAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for warming up caches on application startup
 * Prevents cold start performance issues
 *
 * Cache warming strategy:
 * - Load most frequently accessed data
 * - Populate branch information
 * - Pre-cache active customers
 */
@Slf4j
@Service
public class CacheWarmingService {

    private final CustomerAccountService customerAccountService;
    private final CustomerAccountRepository customerAccountRepository;
    private final BranchAssignmentService branchAssignmentService;

    public CacheWarmingService(
            CustomerAccountService customerAccountService,
            CustomerAccountRepository customerAccountRepository,
            BranchAssignmentService branchAssignmentService) {
        this.customerAccountService = customerAccountService;
        this.customerAccountRepository = customerAccountRepository;
        this.branchAssignmentService = branchAssignmentService;
    }

    /**
     * Warm up caches on application startup
     * Runs asynchronously to not block startup
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCaches() {
        log.info("=== Starting Cache Warming Process ===");
        long startTime = System.currentTimeMillis();

        try {
            // Warm up customer caches
            warmUpCustomerCaches();

            // Warm up branch caches
            warmUpBranchCaches();

            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Cache Warming Completed in {}ms ===", duration);

        } catch (Exception e) {
            log.error("Error during cache warming. Application will continue.", e);
        }
    }

    /**
     * Warm up customer-related caches
     * Loads most recent customers to populate cache
     */
    private void warmUpCustomerCaches() {
        log.info("Warming up customer caches...");

        try {
            // Load recent customers (last 100)
            List<CustomerAccountEntity> recentCustomers = customerAccountRepository
                    .findAll()
                    .stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .limit(100)
                    .toList();

            log.info("Found {} recent customers to cache", recentCustomers.size());

            // Cache each customer by ID
            int cachedCount = 0;
            for (CustomerAccountEntity customer : recentCustomers) {
                try {
                    customerAccountService.getCustomerByCustomerId(customer.getCustomerId());
                    customerAccountService.getCustomerByApplicationId(customer.getApplicationId());
                    cachedCount++;
                } catch (Exception e) {
                    log.warn("Failed to cache customer {}: {}",
                            customer.getCustomerId(), e.getMessage());
                }
            }

            log.info("Successfully cached {} customers", cachedCount);

        } catch (Exception e) {
            log.error("Error warming up customer caches", e);
        }
    }

    /**
     * Warm up branch-related caches
     * Pre-loads branch information for all active branches
     */
    private void warmUpBranchCaches() {
        log.info("Warming up branch caches...");

        try {
            // Get all unique branch codes from customers
            List<String> branchCodes = customerAccountRepository.findAll()
                    .stream()
                    .map(CustomerAccountEntity::getBranchCode)
                    .filter(code -> code != null && !code.isEmpty())
                    .distinct()
                    .toList();

            log.info("Found {} unique branches to cache", branchCodes.size());

            // Cache branch information and customer lists
            int cachedCount = 0;
            for (String branchCode : branchCodes) {
                try {
                    // This will cache both branch info and customer list
                    customerAccountService.getCustomersByBranch(branchCode);
                    cachedCount++;
                } catch (Exception e) {
                    log.warn("Failed to cache branch {}: {}",
                            branchCode, e.getMessage());
                }
            }

            log.info("Successfully cached {} branches", cachedCount);

        } catch (Exception e) {
            log.error("Error warming up branch caches", e);
        }
    }

    /**
     * Manual cache warming trigger (for administrative use)
     * Can be called via JMX or scheduled task
     */
    public void manualWarmUp() {
        log.info("Manual cache warm-up triggered");
        warmUpCaches();
    }

    /**
     * Selective cache warming for specific branch
     * Useful when a new branch is created
     */
    public void warmUpBranch(String branchCode) {
        log.info("Warming up cache for branch: {}", branchCode);

        try {
            customerAccountService.getCustomersByBranch(branchCode);
            log.info("Successfully warmed up cache for branch: {}", branchCode);
        } catch (Exception e) {
            log.error("Failed to warm up cache for branch: {}", branchCode, e);
        }
    }

    /**
     * Selective cache warming for specific customer
     * Useful after data migration or bulk imports
     */
    public void warmUpCustomer(Integer customerId) {
        log.info("Warming up cache for customer: {}", customerId);

        try {
            customerAccountService.getCustomerByCustomerId(customerId);
            log.info("Successfully warmed up cache for customer: {}", customerId);
        } catch (Exception e) {
            log.error("Failed to warm up cache for customer: {}", customerId, e);
        }
    }
}