package com.store.store.constants;

public class ApplicationConstants {

    private ApplicationConstants() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static final String JWT_SECRET_KEY = "JWT_SECRET";
    public static final String JWT_SECRET_DEFAULT_VALUE = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4";
    public static final String JWT_HEADER = "Authorization";

    public static final String ORDER_STATUS_CREATED = "CREATED";
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_STATUS_SHIPPED = "SHIPPED";
    public static final String ORDER_STATUS_DELIVERED = "DELIVERED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";
    public static final String ORDER_STATUS_PENDING = "PENDING";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    public static final String OPEN_MESSAGE = "OPEN";
    public static final String CLOSED_MESSAGE = "CLOSED";
    public static final String IN_PROGRESS_MESSAGE = "IN_PROGRESS";
}