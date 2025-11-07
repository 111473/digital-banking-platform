package com.banking.accountopening.enums;

public enum ApplicationStatus {
    SUBMITTED("Submitted"),
    PENDING("Submitted, awaiting review"),
    UNDER_REVIEW("Application under review"),
    APPROVED("Application accepted, account will be created"),
    REJECTED("Application denied"),
    CANCELLED("Withdrawn by the applicant");

    private final String description;

    ApplicationStatus(String description){
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
