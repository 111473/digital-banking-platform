package com.banking.customeraccount.dto;

import lombok.*;

/**
 * DTO for branch information returned with customer details
 * This provides essential branch details without exposing internal IDs
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchInfoDTO {

    private String branchCode;
    private String branchName;
    private String region;
    private String province;
    private String city;
    private String address;
    private String postalCode;
    private String managerName;
    private String contactNumber;
    private String email;
    private String status;
}

