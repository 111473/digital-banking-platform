package com.banking.customeraccount.enums;


public enum IdentityType {
    PASSPORT("Passport"),
    DRIVER_LICNESE("Driver's License"),
    NATIONAL_ID("National ID");

    private final String description;

    IdentityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
