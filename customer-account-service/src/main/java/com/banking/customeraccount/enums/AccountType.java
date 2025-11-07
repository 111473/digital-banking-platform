package com.banking.customeraccount.enums;

public enum AccountType {

    SAVINGS("Savings Account"),
    CURRENT("Current Account"),
    TIME_DEPOSIT("Time Deposit"),
    JOIN_ACCOUNT("Join Account");

    private final String description;

    AccountType(String description){
        this.description = description;

    }

    public String getDescription() {
        return description;
    }

}
