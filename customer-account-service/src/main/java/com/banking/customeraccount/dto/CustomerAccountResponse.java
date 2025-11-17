package com.banking.customeraccount.dto;

import com.banking.customeraccount.enums.AccountType;
import com.banking.customeraccount.enums.CurrencyType;
import com.banking.customeraccount.enums.KYCStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for customer account information.
 *
 * This is returned when querying customer accounts.
 * It hides internal database IDs and sensitive information.
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountResponse {

    private Integer customerId;
    private Integer applicationId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private AccountType accountType;
    private CurrencyType currencyType;
    private KYCStatus kycStatus;
    private LocalDateTime createdAt;

    // Branch information
    private String branchCode;
    private BranchInfoDTO branchInfo;
}