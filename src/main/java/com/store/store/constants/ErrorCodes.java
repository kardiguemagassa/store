package com.store.store.constants;

import com.store.store.dto.common.ApiResponse;

/**
 * @author Kardigué
 * @version 2.0
 * @since 2025-01-01
 *
 * @see ApiResponse
 * @see com.store.store.exception.GlobalExceptionHandler
 */
public final class ErrorCodes {

    // Constructeur privé pour empêcher l'instanciation
    private ErrorCodes() {
        throw new UnsupportedOperationException("Cette classe ne peut pas être instanciée");
    }

    // VALIDATION ERRORS (4xx - Client Errors)
    // Erreurs de validation des données entrantes

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String TYPE_MISMATCH = "TYPE_MISMATCH";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String INVALID_FORMAT = "INVALID_FORMAT_ERROR";
    public static final String INVALID_JSON = "INVALID_JSON";

    // BUSINESS ERRORS (4xx - Client Errors)
    // Erreurs de logique métier

    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String BUSINESS_RULE_VIOLATION = "BUSINESS_RULE_VIOLATION";
    public static final String VALIDATION_BUSINESS_ERROR = "VALIDATION_BUSINESS_ERROR";
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String ORDER_ALREADY_PROCESSED = "ORDER_ALREADY_PROCESSED";

    // AUTHENTICATION ERRORS (401 - Unauthorized)
    // Erreurs d'authentification

    public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";

    // AUTHORIZATION ERRORS (403 - Forbidden)
    // Erreurs d'autorisation

    public static final String ACCESS_DENIED = "ACCESS_DENIED";

    // HTTP PROTOCOL ERRORS (4xx)
    // Erreurs liées au protocole HTTP


    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";

    // TECHNICAL ERRORS (5xx - Server Errors)
    // Erreurs techniques du serveur

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";
    public static final String EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";

    // MÉTHODES UTILITAIRES

    /**
     * Catégorie d'un code d'erreur.
     */
    public enum ErrorCategory {
        /** Erreurs de validation des données (4xx) */
        VALIDATION,
        /** Erreurs de règles métier (4xx) */
        BUSINESS,
        /** Erreurs d'authentification (401) */
        AUTHENTICATION,
        /** Erreurs d'autorisation (403) */
        AUTHORIZATION,
        /** Erreurs techniques serveur (5xx) */
        TECHNICAL,
        /** Erreurs HTTP protocole (4xx) */
        HTTP_PROTOCOL,
        /** Catégorie inconnue */
        UNKNOWN
    }

    public static ErrorCategory getCategoryOf(String errorCode) {
        if (errorCode == null) {
            return ErrorCategory.UNKNOWN;
        }

        // Validation errors
        if (errorCode.equals(VALIDATION_ERROR) ||
                errorCode.equals(TYPE_MISMATCH) ||
                errorCode.equals(CONSTRAINT_VIOLATION) ||
                errorCode.equals(INVALID_FORMAT) ||
                errorCode.equals(INVALID_JSON)) {
            return ErrorCategory.VALIDATION;
        }

        // Business errors
        if (errorCode.equals(RESOURCE_NOT_FOUND) ||
                errorCode.equals(BUSINESS_RULE_VIOLATION) ||
                errorCode.equals(VALIDATION_BUSINESS_ERROR) ||
                errorCode.equals(INSUFFICIENT_STOCK) ||
                errorCode.equals(ORDER_ALREADY_PROCESSED)) {
            return ErrorCategory.BUSINESS;
        }

        // Authentication errors
        if (errorCode.equals(AUTHENTICATION_REQUIRED) ||
                errorCode.equals(INVALID_CREDENTIALS) ||
                errorCode.equals(AUTHENTICATION_FAILED) ||
                errorCode.equals(ACCOUNT_DISABLED) ||
                errorCode.equals(ACCOUNT_LOCKED) ||
                errorCode.equals(UNAUTHORIZED)) {
            return ErrorCategory.AUTHENTICATION;
        }

        // Authorization errors
        if (errorCode.equals(ACCESS_DENIED)) {
            return ErrorCategory.AUTHORIZATION;
        }

        // HTTP Protocol errors
        if (errorCode.equals(UNSUPPORTED_MEDIA_TYPE) ||
                errorCode.equals(METHOD_NOT_ALLOWED)) {
            return ErrorCategory.HTTP_PROTOCOL;
        }

        // Technical errors
        if (errorCode.equals(INTERNAL_ERROR) ||
                errorCode.equals(CONFIGURATION_ERROR) ||
                errorCode.equals(EXTERNAL_SERVICE_ERROR) ||
                errorCode.equals(DATABASE_ERROR)) {
            return ErrorCategory.TECHNICAL;
        }

        return ErrorCategory.UNKNOWN;
    }

    public static boolean isValidationError(String errorCode) {
        return getCategoryOf(errorCode) == ErrorCategory.VALIDATION;
    }

    public static boolean isBusinessError(String errorCode) {
        return getCategoryOf(errorCode) == ErrorCategory.BUSINESS;
    }

    public static boolean isAuthenticationError(String errorCode) {
        return getCategoryOf(errorCode) == ErrorCategory.AUTHENTICATION;
    }

    public static boolean isTechnicalError(String errorCode) {
        return getCategoryOf(errorCode) == ErrorCategory.TECHNICAL;
    }


    public static boolean isClientError(String errorCode) {
        ErrorCategory category = getCategoryOf(errorCode);
        return category == ErrorCategory.VALIDATION ||
                category == ErrorCategory.BUSINESS ||
                category == ErrorCategory.AUTHENTICATION ||
                category == ErrorCategory.AUTHORIZATION ||
                category == ErrorCategory.HTTP_PROTOCOL;
    }

    public static boolean isServerError(String errorCode) {
        return getCategoryOf(errorCode) == ErrorCategory.TECHNICAL;
    }
}