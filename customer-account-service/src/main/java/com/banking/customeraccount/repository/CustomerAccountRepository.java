package com.banking.customeraccount.repository;

import com.banking.customeraccount.entity.CustomerAccountEntity;
import com.banking.customeraccount.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CustomerAccount entities
 * UPDATED: Added branch-related queries
 */
@Repository
public interface CustomerAccountRepository extends JpaRepository<CustomerAccountEntity, Long> {

    Optional<CustomerAccountEntity> findByCustomerId(Integer customerId);

    Optional<CustomerAccountEntity> findByApplicationId(Integer applicationId);

    Optional<CustomerAccountEntity> findByEmail(String email);

    List<CustomerAccountEntity> findByAccountType(AccountType accountType);

    List<CustomerAccountEntity> findByNameContainingIgnoreCase(String name);

    boolean existsByEmail(String email);

    boolean existsByApplicationId(Integer applicationId);

    long countByAccountType(AccountType accountType);

    // ===== NEW: Branch-related queries =====

    /**
     * Find all customers belonging to a specific branch
     * Usage: customerAccountRepository.findByBranchCode("BR001")
     */
    List<CustomerAccountEntity> findByBranchCode(String branchCode);

    /**
     * Count customers by branch
     * Usage: customerAccountRepository.countByBranchCode("BR001")
     */
    long countByBranchCode(String branchCode);

    /**
     * Check if any customers exist for a branch
     * Usage: customerAccountRepository.existsByBranchCode("BR001")
     */
    boolean existsByBranchCode(String branchCode);
}