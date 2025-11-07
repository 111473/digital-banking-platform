package com.banking.bankaccount.enums;

public enum AccountType {

    SAVINGS( "01", "Savings Account"),
    CURRENT("02", "Current Account"),
    TIME_DEPOSIT("03", "Time Deposit"),
    JOIN_ACCOUNT("04", "Join Account");

    private final String description;
    private final String code;

    AccountType(String code, String description){
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

}
