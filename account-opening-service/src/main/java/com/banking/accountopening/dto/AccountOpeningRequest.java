package com.banking.accountopening.dto;

import com.banking.accountopening.enums.AccountType;
import com.banking.accountopening.enums.CurrencyType;
import com.banking.accountopening.enums.IdentityType;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for creating a new account opening application.
 *
 * This object is sent by the client when applying for a new account.
 * Validation annotations ensure data quality before processing.
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountOpeningRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Currency type is required")
    private CurrencyType currencyType;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Identity type is required")
    private IdentityType identityType;

    @NotBlank(message = "ID reference number is required")
    private String idRefNumber;

}
