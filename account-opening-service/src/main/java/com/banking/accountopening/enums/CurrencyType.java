package com.banking.accountopening.enums;

public enum CurrencyType {
    USD("US Dollar"),
    EUR("Euro"),
    GBP("Pound Sterling"),
    JPY("Japanese Yen"),
    INR("Indian Rupee"),
    AUD("Australian Dollar"),
    CAD("Canadian Dollar"),
    CHF("Swiss Franc"),
    CNY("Chinese Yuan"),
    SEK("Swedish Krona"),
    NZD("New Zealand Dollar");

    private final String description;

    CurrencyType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
