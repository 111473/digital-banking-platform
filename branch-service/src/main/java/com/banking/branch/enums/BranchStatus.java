package com.banking.branch.enums;

/**
 * Enum representing the operational status of a branch
 */
public enum BranchStatus {
    ACTIVE("Branch is operational and accepting customers"),
    INACTIVE("Branch is temporarily closed"),
    UNDER_MAINTENANCE("Branch is undergoing maintenance"),
    CLOSED("Branch is permanently closed");

    private final String description;

    BranchStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}