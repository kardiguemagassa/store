package com.store.store.enums;

public enum ErrorCode {
    // Validation
    VALIDATION_ERROR("VALIDATION_ERROR", ErrorCategory.VALIDATION),
    TYPE_MISMATCH("TYPE_MISMATCH", ErrorCategory.VALIDATION),

    // Business
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", ErrorCategory.BUSINESS),

    // Security
    ACCESS_DENIED("ACCESS_DENIED", ErrorCategory.SECURITY);

    private final String code;
    private final ErrorCategory category;

    ErrorCode(String code, ErrorCategory category) {
        this.code = code;
        this.category = category;
    }

    public String getCode() { return code; }
    public ErrorCategory getCategory() { return category; }
}

enum ErrorCategory {
    VALIDATION, BUSINESS, SECURITY, TECHNICAL
}
