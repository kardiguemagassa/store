package com.store.store.exception;

import com.store.store.constants.ErrorCodes;
import com.store.store.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // Gestion centralisée avec factory methods
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(
            MethodArgumentNotValidException exception, WebRequest webRequest) {

        log.warn("Validation error for request: {}", extractPath(webRequest));

        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, this::getEnhancedValidationMessage)); // Utilise vos messages.properties

        //UTILISATION factory method
        ErrorResponseDto errorResponse = ErrorResponseDto.badRequest(
                ErrorCodes.VALIDATION_ERROR,
                getLocalizedMessage("validation.error.message"), //Message externalisé
                extractPath(webRequest),
                errors

        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException exception, WebRequest webRequest) {

        log.warn("Constraint violation for request: {}", extractPath(webRequest));

        Map<String, String> errors = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> extractFieldName(violation.getPropertyPath().toString()),
                        violation -> {
                            String fieldName = extractFieldName(violation.getPropertyPath().toString());
                            String fieldLabel = getFieldLabel(fieldName);
                            return String.format("%s : %s", fieldLabel, violation.getMessage());
                        }
                ));

        return ResponseEntity.badRequest().body(
                ErrorResponseDto.badRequest(
                        ErrorCodes.CONSTRAINT_VIOLATION,
                        getLocalizedMessage("validation.constraint.violation"),
                        extractPath(webRequest),
                        errors
                )
        );
    }

    //Gestion des type mismatch
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception, WebRequest webRequest) {

        log.warn("Type mismatch for parameter: {} - Path: {}",
                exception.getName(), extractPath(webRequest));

        String fieldLabel = getFieldLabel(exception.getName());
        String expectedType = exception.getRequiredType() != null ?
                exception.getRequiredType().getSimpleName() : "valide";

        // Utilisation d'un message externalisé et spécifique
        String errorMessage = getLocalizedMessage("validation.type.mismatch",
                fieldLabel, expectedType);

        return ResponseEntity.badRequest().body(
                ErrorResponseDto.badRequest(
                        ErrorCodes.TYPE_MISMATCH,  // Code d'erreur spécifique
                        errorMessage,           // Message spécifique utilisé
                        extractPath(webRequest)
                )
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(
            ResourceNotFoundException exception, WebRequest webRequest) {

        log.warn("Resource not found: {} - Path: {}", exception.getMessage(), extractPath(webRequest));

        ErrorResponseDto errorResponse = ErrorResponseDto.notFound(
                ErrorCodes.RESOURCE_NOT_FOUND,                  // Code métier
                exception.getMessage(),
                extractPath(webRequest));


        return new ResponseEntity<>(errorResponse,HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException exception, WebRequest webRequest) {

        log.warn("Access denied for path: {} - User: {}", extractPath(webRequest), exception.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.forbidden(
               ErrorCodes.ACCESS_DENIED,
                exception.getMessage(),
                extractPath(webRequest));

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);

    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessException(BusinessException exception, WebRequest webRequest) {

        log.warn("Business rule violation: {}", exception.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.badRequest(
                ErrorCodes.BUSINESS_RULE_VIOLATION,
                exception.getMessage(),
                extractPath(webRequest));

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            ValidationException exception, WebRequest webRequest) {

        log.warn("Business validation error: {}", exception.getMessage());

        ErrorResponseDto errorResponse = ErrorResponseDto.badRequest(
                ErrorCodes.VALIDATION_ERROR,
                exception.getMessage(),
                extractPath(webRequest),
                exception.getErrors());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(
            Exception exception, WebRequest webRequest) {

        String path = extractPath(webRequest);
        String traceId = generateTraceId();

        log.error("Unexpected error at path: {} - TraceId: {} - Error: {}",
                path, traceId, exception.getMessage(), exception);

        ErrorResponseDto errorResponse = ErrorResponseDto.internalServerError(
                ErrorCodes.INTERNAL_ERROR,
                getLocalizedMessage("error.internal.server"),
                path,
                traceId
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Méthodes utilitaires
    private String extractPath(WebRequest webRequest) {
        if (webRequest.getDescription(false).startsWith("uri=")) {
            return webRequest.getDescription(false).substring(4);
        }
        return webRequest.getDescription(false);
    }

    private String extractFieldName(String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }

    private String getUserFriendlyMessage(FieldError fieldError) {
        try {
            return messageSource.getMessage(fieldError, Locale.getDefault());
        } catch (Exception e) {
            return fieldError.getDefaultMessage() != null ?
                    fieldError.getDefaultMessage() : "Erreur de validation";
        }
    }

    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString();
    }

    // Nouvelle méthode améliorée
    private String getEnhancedValidationMessage(FieldError fieldError) {
        String fieldLabel = getFieldLabel(fieldError.getField());
        String errorCode = fieldError.getCode();

        switch (errorCode) {
            case "NotBlank":
            case "NotNull":
            case "NotEmpty":
                return getLocalizedMessage("validation.required", fieldLabel);

            case "Size":
                Object[] args = fieldError.getArguments();
                if (args != null && args.length >= 2) {
                    Integer max = (Integer) args[1];
                    Integer min = (Integer) args[2];
                    return getLocalizedMessage("validation.size.min.max", fieldLabel, max, min);
                }
                break;

            case "Email":
                return getLocalizedMessage("validation.email", fieldLabel);

            case "Pattern":
                // Messages spécifiques par champ pour les patterns
                if ("mobileNumber".equals(fieldError.getField())) {
                    return getLocalizedMessage("validation.mobileNumber.pattern");
                }
                return getLocalizedMessage("validation.pattern", fieldLabel);

            case "Min":
                Object[] minArgs = fieldError.getArguments();
                if (minArgs != null && minArgs.length >= 1) {
                    return getLocalizedMessage("validation.min.value", fieldLabel, minArgs[0]);
                }
                break;
        }

        // Fallback au message par défaut
        try {
            return messageSource.getMessage(fieldError, Locale.getDefault());
        } catch (Exception e) {
            return fieldError.getDefaultMessage() != null ?
                    fieldError.getDefaultMessage() : "Erreur de validation";
        }
    }

    private String getFieldLabel(String fieldName) {
        return messageSource.getMessage("field." + fieldName, null, fieldName, Locale.getDefault());
    }

    private String getLocalizedMessage(String code, Object... args) {
        // Utilisation de la localisation dynamique
        return messageSource.getMessage(code, args, code, Locale.getDefault());
    }
}