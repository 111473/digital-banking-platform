package com.banking.transaction.enums;

public enum AccountStatus {

    ACTIVE("Fully usable account"),
    Inactive("Dormant due to inactivity"),
    SUSPENDED("Restricted until customer action is taken"),
    CLOSED("Closed"),
    FROZEN("Blocked (suspicious activity, compliance reasons)");

    private final String description;

    AccountStatus(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
