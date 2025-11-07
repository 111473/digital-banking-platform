package com.banking.accountopening.enums;

public enum KYCStatus {
    PENDING("Pending"),
    VERIFIED("Verified"),
    REJECTED("Rejected");

    private final String description;

    KYCStatus(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
