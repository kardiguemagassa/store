package com.store.store.constants;

public class ApplicationConstants {

    private ApplicationConstants() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    public static final String JWT_SECRET_KEY = "JWT_SECRET";
    public static final String JWT_SECRET_DEFAULT_VALUE = "jxgEQeXHuPq8VdbyYFNkANdudQ53YUn4";
    public static final String JWT_HEADER = "Authorization";

    public static final String  ORDER_STATUS_CONFIRMED = "CONFIRMÉ";
    public static final String  ORDER_STATUS_CREATED = "CRÉÉ";
    public static final String  ORDER_STATUS_CANCELLED = "ANNULÉ";
    public static final String ORDER_STATUS_PENDING = "EN ATTENTE";


    public static final String  OPEN_MESSAGE = "OUVRIR";
    public static final String  CLOSED_MESSAGE = "FERMÉ";
}

