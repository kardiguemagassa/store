package com.store.store.exception;

import com.store.store.constants.ErrorCodes;
import com.store.store.dto.common.ApiResponse;

import com.store.store.service.impl.MessageServiceImpl;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Kardigué
 * @version 2.0
 * @since 2025-01-01
 *
 * @see ApiResponse
 * @see MessageServiceImpl
 * @see ErrorCodes
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageServiceImpl messageService;

    // 1. VALIDATION EXCEPTIONS (4xx - Client Errors)
    // Fréquence : TRÈS ÉLEVÉE

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Validation error for request: {} - Total errors: {}",
                path,
                exception.getBindingResult().getFieldErrorCount());

        // Construction de la map des erreurs : {field: message}
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        this::getEnhancedValidationMessage,
                        (existing, replacement) -> existing // En cas de doublon, garder le premier
                ));

        // Message global localisé
        String globalMessage = messageService.getMessage("validation.error.message");

        ApiResponse<Void> response = ApiResponse.validationError(
                ErrorCodes.VALIDATION_ERROR,
                globalMessage,
                path,
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Constraint violation for request: {} - Total violations: {}",
                path,
                exception.getConstraintViolations().size());

        Map<String, String> errors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> extractFieldName(violation.getPropertyPath().toString()),
                        violation -> {
                            String fieldName = extractFieldName(violation.getPropertyPath().toString());
                            String fieldLabel = messageService.getMessage("field." + fieldName, fieldName);
                            return String.format("%s : %s", fieldLabel, violation.getMessage());
                        },
                        (existing, replacement) -> existing
                ));

        String globalMessage = messageService.getMessage("validation.constraint.violation");

        ApiResponse<Void> response = ApiResponse.validationError(
                ErrorCodes.CONSTRAINT_VIOLATION,
                globalMessage,
                path,
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            ValidationException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Business validation error: {} - Path: {}", exception.getMessage(), path);

        ApiResponse<Void> response = ApiResponse.validationError(
                ErrorCodes.VALIDATION_ERROR,
                exception.getMessage(),
                path,
                exception.getErrors()
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Malformed JSON request - Path: {} - Error: {}",
                path,
                exception.getMostSpecificCause().getMessage());

        String message = messageService.getMessage("validation.json.format.error");

        ApiResponse<Void> response = ApiResponse.badRequest(
                ErrorCodes.INVALID_JSON,
                message,
                path
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Type mismatch for parameter: {} - Expected: {} - Received: {} - Path: {}",
                exception.getName(),
                exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "unknown",
                exception.getValue(),
                path);

        String fieldLabel = messageService.getMessage("field." + exception.getName(), exception.getName());
        String expectedType = exception.getRequiredType() != null ?
                exception.getRequiredType().getSimpleName() : "valide";

        String message = messageService.getMessage(
                "validation.type.mismatch",
                fieldLabel,
                expectedType
        );

        ApiResponse<Void> response = ApiResponse.badRequest(
                ErrorCodes.TYPE_MISMATCH,
                message,
                path
        );

        return ResponseEntity.badRequest().body(response);
    }

    // 2. BUSINESS EXCEPTIONS (4xx - Client Errors)
    // Fréquence : ÉLEVÉE


    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Business rule violation: {} - Path: {}", exception.getMessage(), path);

        ApiResponse<Void> response = ApiResponse.badRequest(
                ErrorCodes.BUSINESS_RULE_VIOLATION,
                exception.getMessage(),
                path
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Resource not found: {} - Path: {}", exception.getMessage(), path);

        ApiResponse<Void> response = ApiResponse.notFound(
                ErrorCodes.RESOURCE_NOT_FOUND,
                exception.getMessage(),
                path
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 3. AUTHENTICATION EXCEPTIONS (401 - Unauthorized)
    // Fréquence : MOYENNE

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Invalid credentials attempt - Path: {}", path);

        String message = messageService.getMessage("api.error.auth.bad.credentials");

        ApiResponse<Void> response = ApiResponse.unauthorized(
                ErrorCodes.INVALID_CREDENTIALS,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDisabled(
            DisabledException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Disabled account login attempt - Path: {}", path);

        String message = messageService.getMessage("api.error.auth.account.disabled");

        ApiResponse<Void> response = ApiResponse.unauthorized(
                ErrorCodes.ACCOUNT_DISABLED,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(
            LockedException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Locked account login attempt - Path: {}", path);

        String message = messageService.getMessage("api.error.auth.account.locked");

        ApiResponse<Void> response = ApiResponse.unauthorized(
                ErrorCodes.ACCOUNT_LOCKED,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Authentication failed: {} - Path: {}", exception.getMessage(), path);

        String message = messageService.getMessage("api.error.auth.failed");

        ApiResponse<Void> response = ApiResponse.unauthorized(
                ErrorCodes.AUTHENTICATION_FAILED,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 4. AUTHORIZATION EXCEPTIONS (403 - Forbidden)
    // Fréquence : MOYENNE

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Access denied for path: {} - Reason: {}", path, exception.getMessage());

        String message = messageService.getMessage("api.error.access.denied");

        ApiResponse<Void> response = ApiResponse.forbidden(
                ErrorCodes.ACCESS_DENIED,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 5. HTTP PROTOCOL EXCEPTIONS (4xx)
    // Fréquence : FAIBLE

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Unsupported media type for path: {} - Content-Type: {}",
                path,
                exception.getContentType());

        String message = messageService.getMessage("validation.unsupported.media.type");

        ApiResponse<Void> response = ApiResponse.unsupportedMediaType(
                ErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("HTTP method not supported for path: {} - Method: {} - Supported: {}",
                path,
                exception.getMethod(),
                exception.getSupportedHttpMethods());

        String message = messageService.getMessage("error.method.not.supported");

        ApiResponse<Void> response = ApiResponse.methodNotAllowed(
                ErrorCodes.METHOD_NOT_ALLOWED,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);

        log.warn("Endpoint not found: {} - Path: {}", exception.getResourcePath(), path);

        String message = messageService.getMessage("error.page.not.found");

        ApiResponse<Void> response = ApiResponse.notFound(
                ErrorCodes.RESOURCE_NOT_FOUND,
                message,
                path
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 6. CONFIGURATION EXCEPTIONS (5xx - Server Errors)
    // Fréquence : TRÈS FAIBLE (idéalement ZÉRO en production)

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConfigurationException(
            ConfigurationException exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);
        String traceId = generateTraceId();

        log.error("Configuration error at path: {} - TraceId: {} - Error: {}",
                path, traceId, exception.getMessage(), exception);

        String message = messageService.getMessage("error.configuration");

        ApiResponse<Void> response = ApiResponse.internalError(
                ErrorCodes.CONFIGURATION_ERROR,
                message,
                path,
                traceId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // 7. GLOBAL EXCEPTION HANDLER (Filet de sécurité final)

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception exception,
            WebRequest webRequest) {

        String path = extractPath(webRequest);
        String traceId = generateTraceId();

        // Filtrage des exceptions de test pour réduire le bruit des logs
        boolean isExpectedTestException = exception.getMessage() != null && (
                exception.getMessage().contains("Authentication service unavailable") ||
                        exception.getMessage().contains("Erreur de configuration du système")
        );

        if (!isExpectedTestException) {
            // Log complet avec stack trace - ATTENTION en production !
            log.error("Unexpected error at path: {} - TraceId: {} - Type: {} - Message: {}",
                    path,
                    traceId,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception); // Stack trace complète
        } else {
            log.debug("Expected test exception: {} - Path: {}", exception.getMessage(), path);
        }

        // Message générique pour le client (pas de détails techniques)
        String message = messageService.getMessage("error.internal.server");

        ApiResponse<Void> response = ApiResponse.internalError(
                ErrorCodes.INTERNAL_ERROR,
                message,
                path,
                traceId
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // MÉTHODES UTILITAIRES PRIVÉES

    private String extractPath(WebRequest webRequest) {
        String description = webRequest.getDescription(false);
        return description.startsWith("uri=") ? description.substring(4) : description;
    }

    private String extractFieldName(String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    private String getEnhancedValidationMessage(FieldError fieldError) {
        String fieldName = fieldError.getField();
        String fieldLabel = messageService.getMessage("field." + fieldName, fieldName);
        String errorCode = fieldError.getCode();

        // Gestion des contraintes de validation courantes
        switch (errorCode) {
            case "NotBlank":
            case "NotNull":
            case "NotEmpty":
                return messageService.getMessage("validation.required", fieldLabel);

            case "Size":
                Object[] sizeArgs = fieldError.getArguments();
                if (sizeArgs != null && sizeArgs.length >= 3) {
                    Integer max = (Integer) sizeArgs[1];
                    Integer min = (Integer) sizeArgs[2];
                    return messageService.getMessage("validation.size.range", fieldLabel, max, min);
                }
                break;

            case "Email":
                return messageService.getMessage("validation.email", fieldLabel);

            case "Pattern":
                // Messages spécifiques pour certains champs
                if ("mobileNumber".equals(fieldName)) {
                    return messageService.getMessage("validation.mobileNumber.pattern");
                }
                return messageService.getMessage("validation.pattern", fieldLabel);

            case "Min":
                Object[] minArgs = fieldError.getArguments();
                if (minArgs != null && minArgs.length >= 2) {
                    return messageService.getMessage("validation.min.value", fieldLabel, minArgs[1]);
                }
                break;

            case "Max":
                Object[] maxArgs = fieldError.getArguments();
                if (maxArgs != null && maxArgs.length >= 2) {
                    return messageService.getMessage("validation.max.value", fieldLabel, maxArgs[1]);
                }
                break;

            case "DecimalMin":
                Object[] decimalMinArgs = fieldError.getArguments();
                if (decimalMinArgs != null && decimalMinArgs.length >= 2) {
                    return messageService.getMessage("validation.decimal.min", fieldLabel, decimalMinArgs[1]);
                }
                break;

            case "DecimalMax":
                Object[] decimalMaxArgs = fieldError.getArguments();
                if (decimalMaxArgs != null && decimalMaxArgs.length >= 2) {
                    return messageService.getMessage("validation.decimal.max", fieldLabel, decimalMaxArgs[1]);
                }
                break;
        }

        // Fallback : retourner le message par défaut du champ s'il existe
        return fieldError.getDefaultMessage() != null ?
                fieldError.getDefaultMessage() : "Erreur de validation";
    }
}