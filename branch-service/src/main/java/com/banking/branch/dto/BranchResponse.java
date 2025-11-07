package com.banking.branch.dto;

import com.banking.branch.enums.BranchStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for Branch information
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {

    private String branchCode;
    private String branchName;
    private String region;
    private String province;
    private String city;
    private String address;
    private String postalCode;
    private String managerName;
    private String email;
    private String contactNumber;
    private BranchStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // NEW: Customer statistics
    private Long totalCustomers;
}