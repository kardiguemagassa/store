package com.store.store.constants;

public final class ValidationPatterns {
    public static final String PAYMENT_STATUS = "^(paid|pending|failed)$";
    public static final String ORDER_STATUS = "^(CREATED|CONFIRMED|CANCELLED|DELIVERED)$";
    public static final String CURRENCY = "^(eur|usd|gbp|cad)$";

    private ValidationPatterns() {}
}
