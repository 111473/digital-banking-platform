package com.banking.branch.repository;

import com.banking.branch.entity.BranchEntity;
import com.banking.branch.enums.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Branch entities
 */
@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {

    Optional<BranchEntity> findByBranchCode(String branchCode);

    List<BranchEntity> findByStatus(BranchStatus status);

    List<BranchEntity> findByRegion(String region);

    List<BranchEntity> findByProvince(String province);

    List<BranchEntity> findByCity(String city);

    boolean existsByBranchCode(String branchCode);

    boolean existsByEmail(String email);

    long countByStatus(BranchStatus status);

    List<BranchEntity> findByBranchNameContainingIgnoreCase(String branchName);
}