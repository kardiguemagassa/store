package com.store.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponseDto {

    private String path;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private String status;          // Bad Request, Not Found human
    private int statusCode;         // 400, 404 machines
    private String errorCode;       // VALIDATION_ERROR, RESOURCE_NOT_FOUND
    private String message;         // Message user friendly
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Europe/Paris")
    private LocalDateTime timestamp;
    // pour débogage
    private Map<String, String> errors;  // Détails validation
    private String traceId;              // Correlation ID

    //FACTORY METHODS
    // with errorCode
    public static ErrorResponseDto of(HttpStatus httpStatus, String errorCode, String message, String path) {
        return ErrorResponseDto.builder()
                .path(path)
                .status(httpStatus.getReasonPhrase())
                .statusCode(httpStatus.value())
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // for validation errors
    public static ErrorResponseDto of(HttpStatus httpStatus, String errorCode, String message,
                                      String path, Map<String, String> errors) {
        return ErrorResponseDto.builder()
                .path(path)
                .status(httpStatus.getReasonPhrase())
                .statusCode(httpStatus.value())
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();
    }

    // for technicals errors
    public static ErrorResponseDto of(HttpStatus httpStatus, String errorCode, String message,
                                      String path, String traceId) {
        return ErrorResponseDto.builder()
                .path(path)
                .status(httpStatus.getReasonPhrase())
                .statusCode(httpStatus.value())
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .build();
    }

    // SHORTCUT METHODS MISE À JOUR
    public static ErrorResponseDto notFound(String errorCode, String message, String path) {
        return of(HttpStatus.NOT_FOUND, errorCode, message, path);
    }

    public static ErrorResponseDto badRequest(String errorCode, String message, String path) {
        return of(HttpStatus.BAD_REQUEST, errorCode, message, path);
    }

    public static ErrorResponseDto badRequest(String errorCode, String message,
                                              String path, Map<String, String> errors) {
        return of(HttpStatus.BAD_REQUEST, errorCode, message, path, errors);
    }

    public static ErrorResponseDto forbidden(String errorCode, String message, String path) {
        return of(HttpStatus.FORBIDDEN, errorCode, message, path);
    }

    public static ErrorResponseDto internalServerError(String errorCode, String message, String path) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, message, path);
    }

    public static ErrorResponseDto internalServerError(String errorCode, String message,
                                                       String path, String traceId) {
        return of(HttpStatus.INTERNAL_SERVER_ERROR, errorCode, message, path, traceId);
    }

    public static ErrorResponseDto unauthorized(String errorCode, String message, String path) {
        return of(HttpStatus.UNAUTHORIZED, errorCode, message, path);
    }
}