package com.store.store.enums;

import lombok.Getter;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready
 * @since 2025-01-06
 */
@Getter
public enum PaymentStatus {

    PAID,
    PENDING,
    FAILED;

    public String getMessageKey() {
        return "payment.status." + name().toLowerCase();
    }

    public boolean isConfirmed() {
        return this == PAID;
    }

    public boolean isFailed() {
        return this == FAILED;
    }

    public boolean isPending() {
        return this == PENDING;
    }
}