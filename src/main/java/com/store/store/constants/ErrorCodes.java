package com.store.store.constants;

public final class ErrorCodes {
    private ErrorCodes() {}

    //VALIDATION ERRORS
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH_ERROR";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String INVALID_FORMAT = "INVALID_FORMAT_ERROR";

    //BUSINESS ERRORS
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String VALIDATION_BUSINESS_ERROR = "VALIDATION_BUSINESS_ERROR"; // ValidationException
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String ORDER_ALREADY_PROCESSED = "ORDER_ALREADY_PROCESSED";

    //SECURITY ERRORS
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

    //TECHNICAL ERRORS
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";

    //UTILITY METHODS
    /**
     * Vérifie si un code d'erreur est de type validation
     */
    public static boolean isValidationError(String errorCode) {
        return errorCode != null &&
                (errorCode.contains("VALIDATION") ||
                        errorCode.contains("TYPE_MISMATCH") ||
                        errorCode.contains("CONSTRAINT") ||
                        errorCode.contains("INVALID_FORMAT"));
    }

    /**
     * Vérifie si un code d'erreur est de type métier
     */
    public static boolean isBusinessError(String errorCode) {
        return errorCode != null &&
                (errorCode.contains("NOT_FOUND") ||
                        errorCode.contains("BUSINESS") ||
                        errorCode.contains("INSUFFICIENT") ||
                        errorCode.contains("ORDER"));
    }
}