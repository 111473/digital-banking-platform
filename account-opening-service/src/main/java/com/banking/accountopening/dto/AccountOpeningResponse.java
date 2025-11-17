package com.banking.accountopening.dto;

import com.banking.accountopening.enums.AccountType;
import com.banking.accountopening.enums.ApplicationStatus;
import com.banking.accountopening.enums.CurrencyType;
import com.banking.accountopening.enums.KYCStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Response DTO for account opening application.
 *
 * This object is returned to the client after creating or retrieving
 * an account opening application. It contains only the information
 * that should be exposed to the client (no internal database IDs).
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountOpeningResponse {

    private Integer applicationId;
    private AccountType accountType;
    private CurrencyType currencyType;
    private ApplicationStatus applicationStatus;
    private KYCStatus kycStatus;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private LocalDateTime applicationDate;
    private LocalDateTime createdAt;

}