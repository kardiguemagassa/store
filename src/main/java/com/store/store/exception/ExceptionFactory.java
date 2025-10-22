package com.store.store.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory pour la création d'exceptions métier standardisées.
 * Centralise la création des exceptions pour assurer la cohérence.
 */
@Slf4j
@Component
public class ExceptionFactory {

    // EXCEPTIONS SPÉCIFIQUES
    public ProductNotFoundException productNotFound(Long productId) {
        log.debug("Creating ProductNotFoundException for productId: {}", productId);
        return new ProductNotFoundException(productId);
    }
    public OrderNotFoundException orderNotFound(Long orderId) {
        log.debug("Creating OrderNotFoundException for orderId: {}", orderId);
        return new OrderNotFoundException(orderId);
    }

    public ContactNotFoundException contactNotFound(Long contactId) {
        log.debug("Creating ContactNotFoundException for contactId: {}", contactId);
        return new ContactNotFoundException(contactId);
    }

    public ResourceNotFoundException resourceNotFound(String resource, String field, String value) {
        log.debug("Creating ResourceNotFoundException: {} not found with {} = {}", resource, field, value);
        return new ResourceNotFoundException(resource, field, value);
    }

    // VALIDATION
    public ValidationException validationError(String field, String message) {
        log.debug("Creating ValidationException for field {}: {}", field, message);
        return new ValidationException(field, message);
    }

    public ValidationException validationErrors(Map<String, String> errors) {
        log.debug("Creating ValidationException with {} errors", errors.size());
        return new ValidationException(errors);
    }

    // BUSINESS EXCEPTIONS
    public BusinessException businessError(String message) {
        log.debug("Creating BusinessException: {}", message);
        return new BusinessException(message);
    }

    public BusinessException businessError(String message, Throwable cause) {
        log.debug("Creating BusinessException: {}", message, cause);
        return new BusinessException(message, cause);
    }
}