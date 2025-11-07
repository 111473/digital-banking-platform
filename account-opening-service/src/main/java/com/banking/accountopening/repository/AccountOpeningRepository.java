package com.banking.accountopening.repository;

import com.banking.accountopening.entity.AccountOpeningEntity;
import com.banking.accountopening.enums.ApplicationStatus;
import com.banking.accountopening.enums.KYCStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AccountOpening entities
 *
 * Spring Data JPA will automatically implement these methods.
 * No need to write implementation code!
 */
@Repository
public interface AccountOpeningRepository extends JpaRepository<AccountOpeningEntity, Long> {

    /**
     * Find application by application ID
     * Usage: accountOpeningRepository.findByApplicationId(100001)
     *
     * @param applicationId The unique application ID
     * @return Optional containing the application if found
     */
    Optional<AccountOpeningEntity> findByApplicationId(Integer applicationId);

    /**
     * Find all applications by status
     * Usage: accountOpeningRepository.findByApplicationStatus(ApplicationStatus.PENDING)
     *
     * Useful for:
     * - Getting all pending applications
     * - Getting all applications under review
     * - Getting all approved applications
     *
     * @param status The application status to filter by
     * @return List of applications with the given status
     */
    List<AccountOpeningEntity> findByApplicationStatus(ApplicationStatus status);

    /**
     * Find all applications by email
     * Usage: accountOpeningRepository.findByEmail("john@example.com")
     *
     * Useful for:
     * - Checking if customer already has an application
     * - Finding customer's application history
     *
     * @param email The customer email
     * @return List of applications for this email
     */
    List<AccountOpeningEntity> findByEmail(String email);

    /**
     * Find applications by KYC status
     * Usage: accountOpeningRepository.findByKycStatus(KYCStatus.PENDING)
     *
     * Useful for:
     * - Finding applications waiting for KYC verification
     * - Getting all verified applications
     *
     * @param kycStatus The KYC status to filter by
     * @return List of applications with the given KYC status
     */
    List<AccountOpeningEntity> findByKycStatus(KYCStatus kycStatus);

    /**
     * Find applications by both application status AND KYC status
     * Usage: findByApplicationStatusAndKycStatus(ApplicationStatus.UNDER_REVIEW, KYCStatus.PENDING)
     *
     * Useful for:
     * - Finding applications under review that need KYC verification
     * - Finding approved applications with verified KYC
     *
     * @param applicationStatus The application status
     * @param kycStatus The KYC status
     * @return List of applications matching both criteria
     */
    List<AccountOpeningEntity> findByApplicationStatusAndKycStatus(
            ApplicationStatus applicationStatus,
            KYCStatus kycStatus
    );

    /**
     * Check if an application exists by email
     * Usage: accountOpeningRepository.existsByEmail("john@example.com")
     *
     * Returns true/false without loading the full entity (more efficient)
     *
     * @param email The customer email
     * @return true if application exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Count applications by status
     * Usage: accountOpeningRepository.countByApplicationStatus(ApplicationStatus.PENDING)
     *
     * Useful for dashboards/statistics
     *
     * @param status The application status
     * @return Number of applications with this status
     */
    long countByApplicationStatus(ApplicationStatus status);
}
