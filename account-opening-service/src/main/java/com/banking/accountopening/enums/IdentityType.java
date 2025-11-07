package com.banking.accountopening.enums;


public enum IdentityType {
    PASSPORT("Passport"),
    DRIVER_LICENSE("Driver's License"),
    NATIONAL_ID("National ID");

    private final String description;

    IdentityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
