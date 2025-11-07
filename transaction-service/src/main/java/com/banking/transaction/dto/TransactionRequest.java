package com.banking.transaction.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


public class TransactionRequest {

    @NotNull(message = "Account number is required")
    private Integer accountNumber;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be positive")
    private Double amount;

    private String description;
    private String referenceNumber;

    // Constructors
    public TransactionRequest() {}

    // Getters and Setters
    public Integer getAccountNumber() { return accountNumber; }
    public void setAccountNumber(Integer accountNumber) { this.accountNumber = accountNumber; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

}