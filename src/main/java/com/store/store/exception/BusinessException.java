package com.store.store.exception;

/**
 * Exception pour les erreurs métier qui ne sont pas des "ressources non trouvées"
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}