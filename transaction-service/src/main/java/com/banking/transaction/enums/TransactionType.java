package com.banking.transaction.enums;

public enum TransactionType {

    WITHDRAWAL("Cash Withdrawal", 10000.0, 5000.0, true, 2),
    DEPOSIT("Cash Deposit", 10000.0, 5000.0, true, 1),
    PAYMENT("Bills Payment", 25000.0, 10000.0, false, 1),
    TRANSFER("Account Transfer", 50000.0, 15000.0, false, 1),
    WIRE_TRANSFER("Wire Transfer", 10000.0, 3000.0, false,3),
    CASH_ADVANCE("Cash Advance", 5000.0, 2000.0, true,2);

    private final String description;
    private final double ctrThreshold;
    private final double sarThreashold;
    private final boolean requiresHandling;
    private final int riskLevel;

    TransactionType(String description, double ctrThreshold, double sarThreashold, boolean requiresHandling, int riskLevel){
        this.description   = description;
        this.ctrThreshold  = ctrThreshold;
        this.sarThreashold = sarThreashold;
        this.requiresHandling = requiresHandling;
        this.riskLevel = riskLevel;
    }

    public String getDescription() {
        return description;
    }

    public double getCtrThreshold() {
        return ctrThreshold;
    }

    public double getSarThreashold() {
        return sarThreashold;
    }

    public boolean isRequiresHandling() {
        return requiresHandling;
    }

    public int getRiskLevel() {
        return riskLevel;
    }
}
