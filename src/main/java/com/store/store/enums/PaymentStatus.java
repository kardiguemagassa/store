package com.store.store.enums;

public enum PaymentStatus {
    PAID("Payé"),
    PENDING("En attente"),
    FAILED("Échoué");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}