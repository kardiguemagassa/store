package com.store.store.enums;

import lombok.Getter;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready
 * @since 2025-01-06
 */
@Getter
public enum OrderStatus {

    CREATED,
    CONFIRMED,
    CANCELLED,
    DELIVERED;


    public String getMessageKey() {
        return "order.status." + name().toLowerCase();
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == DELIVERED;
    }


    public boolean canTransitionTo(OrderStatus newStatus) {
        // États terminaux : aucune transition possible
        if (this.isTerminal()) {
            return false;
        }

        return switch (this) {
            case CREATED -> newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED -> newStatus == DELIVERED || newStatus == CANCELLED;
            default -> false;
        };
    }
}