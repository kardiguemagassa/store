package com.store.store.enums;

public enum OrderStatus {
    CREATED("Créée"),
    CONFIRMED("Confirmée"),
    CANCELLED("Annulée"),
    DELIVERED("Livrée");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}