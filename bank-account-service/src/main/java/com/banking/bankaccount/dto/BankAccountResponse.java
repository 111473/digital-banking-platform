package com.banking.bankaccount.dto;

import com.banking.bankaccount.enums.AccountStatus;
import com.banking.bankaccount.enums.AccountType;
import lombok.*;


/**
 * Response DTO for bank account information.
 */
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResponse {

    private Integer accountNumber;
    private Integer customerId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String branchCode;
    private AccountType accountType;
    private Double balance;
    private Double interestRate;
    private AccountStatus accountStatus;

}