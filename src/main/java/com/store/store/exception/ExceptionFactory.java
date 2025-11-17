package com.store.store.exception;


import com.store.store.service.impl.MessageServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Kardigué
 * @version 2.0
 * @since 2025-01-01
 *
 * @see MessageServiceImpl
 * @see BusinessException
 * @see ResourceNotFoundException
 */
@Component
@RequiredArgsConstructor
public class ExceptionFactory {

    private final MessageServiceImpl messageService;

    // VALIDATION EXCEPTIONS

    public ValidationException validationError(String field, String message) {
        return new ValidationException(field, message);
    }

    public ValidationException validationError(Map<String, String> errors) {
        return new ValidationException(errors);
    }

    public ValidationException validationErrorWithCode(String field, String messageCode, Object... args) {
        String localizedMessage = messageService.getMessage(messageCode, args);
        return new ValidationException(field, localizedMessage);
    }

    // BUSINESS EXCEPTIONS

    public BusinessException businessError(String message) {
        return new BusinessException(message);
    }

    public BusinessException businessErrorWithCode(String messageCode, Object... args) {
        String localizedMessage = messageService.getMessage(messageCode, args);
        return new BusinessException(localizedMessage);
    }

    // RESOURCE NOT FOUND EXCEPTIONS

    public ResourceNotFoundException resourceNotFound(String resourceName, String fieldName, Object fieldValue) {
        String message = messageService.getMessage(
                "api.error.resource.not.found.with.field",
                resourceName,
                fieldName,
                fieldValue
        );
        return new ResourceNotFoundException(message);
    }

    public ResourceNotFoundException resourceNotFoundById(String resourceName, Object id) {
        String message = messageService.getMessage(
                "api.error.resource.not.found.with.id",
                resourceName,
                id
        );
        return new ResourceNotFoundException(message);
    }

    public ResourceNotFoundException resourceNotFoundSimple(String resourceName) {
        String message = messageService.getMessage(
                "api.error.resource.not.found",
                resourceName
        );
        return new ResourceNotFoundException(message);
    }

    public ResourceNotFoundException resourceNotFoundWithCode(String messageCode, Object... args) {
        String localizedMessage = messageService.getMessage(messageCode, args);
        return new ResourceNotFoundException(localizedMessage);
    }

    // ROLE EXCEPTIONS

    public ResourceNotFoundException missingRole(String roleName) {
        String message = messageService.getMessage(
                "api.error.role.not.found",
                roleName
        );
        return new ResourceNotFoundException(message);
    }

    // FILE STORAGE EXCEPTIONS

    public FileStorageException fileStorageError(String message) {
        return new FileStorageException(message);
    }

    public FileStorageException fileStorageError(String message, Throwable cause) {
        return new FileStorageException(message, cause);
    }

    public FileStorageException fileStorageErrorWithCode(String messageCode, Object... args) {
        String localizedMessage = messageService.getMessage(messageCode, args);
        return new FileStorageException(localizedMessage);
    }

    public FileStorageException fileStorageErrorWithCode(String messageCode, Throwable cause, Object... args) {
        String localizedMessage = messageService.getMessage(messageCode, args);
        return new FileStorageException(localizedMessage, cause);
    }

    // CONFIGURATION EXCEPTIONS

    public ConfigurationException configurationError(String message) {
        return new ConfigurationException(message);
    }

    public ConfigurationException configurationError(String message, Throwable cause) {
        return new ConfigurationException(message, cause);
    }

    public ConfigurationException configurationErrorWithCode(String messageCode, Object... args) {
        String localizedMessage = messageService.getMessage(messageCode, args);
        return new ConfigurationException(localizedMessage);
    }
}