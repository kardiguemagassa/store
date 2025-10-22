package com.store.store.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception spécifique pour les erreurs de validation métier
 */
@Getter
public class ValidationException extends BusinessException {
    private final Map<String, String> errors;

    // Pour une seule erreur
    public ValidationException(String field, String message) {
        super("Erreur de validation");
        this.errors = new HashMap<>();
        this.errors.put(field, message);
    }

    // Pour multiples erreurs
    public ValidationException(Map<String, String> errors) {
        super("Erreurs de validation");
        this.errors = new HashMap<>(errors);
    }

    public void addError(String field, String message) {
        this.errors.put(field, message);
    }

}