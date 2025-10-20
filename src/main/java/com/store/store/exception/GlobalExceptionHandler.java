package com.store.store.exception;

import com.store.store.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //EXCEPTIONS MÉTIER INTERCEPTER
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception exception,
                                                                  WebRequest webRequest) {
        log.error("Technical error : {}", exception.getMessage(), exception);

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur technique s'est produite. Notre équipe a été alertée.",
                LocalDateTime.now());
        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException exception) {
        log.warn("Invalid data submitted");

        Map<String, String> fieldErrors = new HashMap<>();
        List<FieldError> fieldErrorList = exception.getBindingResult().getFieldErrors();

        fieldErrorList.forEach(error -> {
            String fieldName = getFieldNameForUser(error.getField());
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });

        // propriété "errors"
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", "Erreur de validation des données");
        response.put("errors", fieldErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        log.warn("Violation of validation constraints");

        Map<String, String> errors = new HashMap<>();
        Set<ConstraintViolation<?>> constraintViolationSet = exception.getConstraintViolations();

        constraintViolationSet.forEach(constraintViolation -> {
            String fieldName = getFieldNameForUser(
                    extractFieldName(constraintViolation.getPropertyPath().toString())
            );
            errors.put(fieldName, constraintViolation.getMessage());
        });

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException exception,
                                                                            WebRequest webRequest){
        log.warn("Resource not found : {}", exception.getMessage());

        ErrorResponseDto errorResponseDTO = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.NOT_FOUND,
                "La ressource demandée n'a pas été trouvée.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(NoResourceFoundException exception,
                                                                           WebRequest webRequest) {
        log.warn("Non-existent URL accessed: {}", exception.getResourcePath());

        String errorMessage = "Désolé, la page que vous recherchez n'existe pas. " +
                "Retournez à la page d'accueil.";

        ErrorResponseDto errorResponseDTO = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.NOT_FOUND,
                errorMessage,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(AccessDeniedException exception,
                                                                        WebRequest webRequest) {
        log.warn("Access denied : {}", exception.getMessage());

        ErrorResponseDto errorResponseDTO = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.FORBIDDEN,
                "Accès refusé. Vous n'avez pas les permissions nécessaires pour accéder à cette ressource.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.FORBIDDEN);
    }

    //EXCEPTIONS HTTP STANDARDS NE PAS INTERCEPTER Spring retourner le code HTTP approprié
    /**
     * Ne pas intercepter HttpRequestMethodNotSupportedException (405)
     * Spring retourner le code HTTP approprié
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public void handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) throws Exception {
        throw ex;
    }

    /**
     * Ne pas intercepter HttpMediaTypeNotSupportedException (415)
     * Spring retourner le code HTTP approprié
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public void handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) throws Exception {
        throw ex;
    }

    /**
     * Ne pas intercepter MissingServletRequestParameterException (400)
     * Spring retourner le code HTTP approprié
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public void handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) throws Exception {
        throw ex;
    }

    /**
     * Ne pas intercepter MethodArgumentTypeMismatchException (400)
     * Spring retourner le code HTTP approprié
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public void handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) throws Exception {
        throw ex;
    }

    /**
     * Ne pas intercepter HttpMessageNotReadableException (400)
     * Spring retourner le code HTTP approprié pour les body invalides
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) throws Exception {
        throw ex;
    }

    // Utility Method All your DTOs
    private String getFieldNameForUser(String technicalFieldName) {
        Map<String, String> fieldMappings = Map.of(
                // ProfileRequestDto
                "name", "Nom",
                "email", "Adresse email",
                "mobileNumber", "Numéro de mobile",
                "street", "Rue",
                "city", "Ville",
                "state", "Département",
                "postalCode", "Code postal",
                "country", "Pays",

                // RegisterRequestDto
                "password", "Mot de passe",

                // ContactRequestDto
                "message", "Message"
        );
        return fieldMappings.getOrDefault(technicalFieldName, technicalFieldName);
    }

    private String extractFieldName(String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : propertyPath;
    }
}