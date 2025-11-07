package com.banking.bankaccount.repository;

import com.banking.bankaccount.entity.BankAccountEntity;
import com.banking.bankaccount.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BankAccount entities
 * ✅ Updated to support branch-related queries
 */
@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, Long> {

    /**
     * Find bank account by account number
     */
    Optional<BankAccountEntity> findByAccountNumber(Integer accountNumber);

    /**
     * Find all bank accounts for a specific customer
     */
    List<BankAccountEntity> findByCustomerId(Integer customerId);

    /**
     * Find first bank account for a customer (for idempotency check)
     */
    Optional<BankAccountEntity> findFirstByCustomerId(Integer customerId);

    /**
     * ✅ NEW: Find all bank accounts for a specific branch
     */
    List<BankAccountEntity> findByBranchCode(String branchCode);

    /**
     * ✅ NEW: Find active bank accounts for a specific branch
     */
    List<BankAccountEntity> findByBranchCodeAndAccountStatus(String branchCode, AccountStatus accountStatus);

    /**
     * Find bank accounts by status
     */
    List<BankAccountEntity> findByAccountStatus(AccountStatus accountStatus);

    /**
     * Find bank accounts by customer ID and status
     */
    List<BankAccountEntity> findByCustomerIdAndAccountStatus(
            Integer customerId,
            AccountStatus accountStatus
    );

    /**
     * Check if account exists by account number
     */
    boolean existsByAccountNumber(Integer accountNumber);

    /**
     * ✅ NEW: Check if accounts exist for a branch
     */
    boolean existsByBranchCode(String branchCode);

    /**
     * Count accounts by status
     */
    long countByAccountStatus(AccountStatus accountStatus);

    /**
     * ✅ NEW: Count accounts by branch code
     */
    long countByBranchCode(String branchCode);

    /**
     * ✅ NEW: Count active accounts by branch code
     */
    long countByBranchCodeAndAccountStatus(String branchCode, AccountStatus accountStatus);

    /**
     * Find accounts with balance greater than specified amount
     */
    List<BankAccountEntity> findByBalanceGreaterThan(Double balance);

    /**
     * Find accounts with balance less than specified amount
     */
    List<BankAccountEntity> findByBalanceLessThan(Double balance);

    /**
     * Custom query: Find all active accounts for a customer
     */
    @Query("SELECT ba FROM BankAccountEntity ba WHERE ba.customerId = :customerId AND ba.accountStatus = 'ACTIVE'")
    List<BankAccountEntity> findActiveAccountsForCustomer(@Param("customerId") Integer customerId);

    /**
     * Custom query: Calculate total balance for a customer
     */
    @Query("SELECT SUM(ba.balance) FROM BankAccountEntity ba WHERE ba.customerId = :customerId")
    Double getTotalBalanceForCustomer(@Param("customerId") Integer customerId);

    /**
     * Custom query: Count accounts by customer
     */
    @Query("SELECT COUNT(ba) FROM BankAccountEntity ba WHERE ba.customerId = :customerId")
    long countAccountsForCustomer(@Param("customerId") Integer customerId);

    /**
     * ✅ NEW: Custom query: Calculate total balance for a branch
     */
    @Query("SELECT SUM(ba.balance) FROM BankAccountEntity ba WHERE ba.branchCode = :branchCode")
    Double getTotalBalanceForBranch(@Param("branchCode") String branchCode);

    /**
     * ✅ NEW: Custom query: Get branch statistics
     */
    @Query("SELECT ba.branchCode, COUNT(ba), SUM(ba.balance), AVG(ba.balance) " +
            "FROM BankAccountEntity ba " +
            "WHERE ba.accountStatus = 'ACTIVE' " +
            "GROUP BY ba.branchCode")
    List<Object[]> getBranchStatistics();
}