package com.banking.branch.dto;

import com.banking.branch.enums.BranchStatus;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for creating/updating branches
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRequest {

    @NotBlank(message = "Branch code is required")
    @Pattern(regexp = "^BR\\d{3,6}$", message = "Branch code must be in format BR### (e.g., BR001)")
    private String branchCode;

    @NotBlank(message = "Branch name is required")
    @Size(min = 3, max = 100)
    private String branchName;

    @NotBlank(message = "Region is required")
    private String region;

    @NotBlank(message = "Province is required")
    private String province;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @NotBlank(message = "Manager name is required")
    private String managerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String contactNumber;

    @NotNull(message = "Status is required")
    private BranchStatus status;
}