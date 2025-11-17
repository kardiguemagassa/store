package com.store.store.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kardigué
 * @version 2.0
 * @since 2025-01-01
 *
 * @see BusinessException
 * @see com.store.store.exception.ExceptionFactory
 * @see com.store.store.exception.GlobalExceptionHandler
 */
@Getter
public class ValidationException extends BusinessException {


    private final Map<String, String> errors;

    public ValidationException(String field, String message) {
        super("Validation error"); // Message technique pour les logs
        this.errors = new HashMap<>();
        this.errors.put(field, message);
    }

    public ValidationException(Map<String, String> errors) {
        super("Validation errors"); // Message technique pour les logs
        this.errors = new HashMap<>(errors); // Copie défensive
    }

    @Deprecated
    public void addError(String field, String message) {
        this.errors.put(field, message);
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    public boolean hasErrorForField(String field) {
        return errors != null && errors.containsKey(field);
    }

    public String getErrorForField(String field) {
        return errors != null ? errors.get(field) : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationException: ")
                .append(getMessage())
                .append(" (")
                .append(getErrorCount())
                .append(" error(s))\n");

        if (errors != null) {
            errors.forEach((field, message) ->
                    sb.append("  - ").append(field).append(": ").append(message).append("\n")
            );
        }

        return sb.toString();
    }
}