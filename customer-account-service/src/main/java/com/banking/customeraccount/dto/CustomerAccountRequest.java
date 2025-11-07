package com.banking.customeraccount.dto;

import com.banking.customeraccount.enums.AccountType;
import com.banking.customeraccount.enums.CurrencyType;
import com.banking.customeraccount.enums.IdentityType;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Request DTO for creating a customer account directly.
 *
 * OPTIONAL: Only needed if you want to allow manual customer creation
 * (bypassing the application approval workflow).
 *
 * Use cases:
 * - Admin manually creates customer
 * - Bulk import of existing customers
 * - Migration from legacy system
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Identity type is required")
    private IdentityType identityType;

    @NotBlank(message = "ID reference number is required")
    private String idRefNumber;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @NotNull(message = "Currency type is required")
    private CurrencyType currencyType;

}