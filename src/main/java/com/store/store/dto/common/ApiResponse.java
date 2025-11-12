package com.store.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO de réponse API unifié.
 *
 * IMPORTANT: Tous les messages utilisent l'internationalisation via messages.properties.
 * Aucun message n'est hardcodé dans ce DTO.
 *
 * @author Kardigué
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Réponse API standardisée")
public class ApiResponse<T> {

    @Schema(description = "Indicateur de succès", example = "true")
    @Builder.Default
    private Boolean success = true;

    @Schema(description = "Message descriptif", example = "Produit créé avec succès")
    private String message;

    @Schema(description = "Code de statut HTTP", example = "200")
    private Integer statusCode;

    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss", timezone = "Europe/Paris")
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "Chemin de l'API", example = "/api/v1/products/123")
    private String path;

    @Schema(description = "Données retournées", nullable = true)
    private T data;

    @Schema(description = "Code d'erreur métier", example = "VALIDATION_ERROR", nullable = true)
    private String errorCode;

    @Schema(description = "Détails des erreurs de validation", nullable = true)
    private Map<String, String> errors;

    @Schema(description = "Identifiant de traçage", nullable = true)
    private String traceId;

    // ========================================================================
    // FACTORY METHODS DE BASE (message déjà localisé)
    // ========================================================================

    /**
     * Crée une réponse de succès.
     *
     * @param localizedMessage Message DÉJÀ localisé (récupéré via MessageSource)
     * @param data Données à retourner
     * @return ApiResponse avec statut 200
     */
    public static <T> ApiResponse<T> success(String localizedMessage, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(localizedMessage)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    /**
     * Crée une réponse de succès sans données.
     */
    public static <T> ApiResponse<T> success(String localizedMessage) {
        return success(localizedMessage, null);
    }

    /**
     * Crée une réponse de création (201).
     */
    public static <T> ApiResponse<T> created(String localizedMessage, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(localizedMessage)
                .statusCode(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    /**
     * Crée une réponse vide (204).
     */
    public static <T> ApiResponse<T> noContent(String localizedMessage) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(localizedMessage)
                .statusCode(HttpStatus.NO_CONTENT.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ========================================================================
    // FACTORY METHODS - ERREURS
    // ========================================================================

    /**
     * Crée une réponse d'erreur.
     */
    public static <T> ApiResponse<T> error(HttpStatus httpStatus, String errorCode,
                                           String localizedMessage, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(localizedMessage)
                .statusCode(httpStatus.value())
                .errorCode(errorCode)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse d'erreur de validation.
     */
    public static <T> ApiResponse<T> validationError(String errorCode, String localizedMessage,
                                                     String path, Map<String, String> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(localizedMessage)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .errorCode(errorCode)
                .path(path)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse d'erreur serveur avec traceId.
     */
    public static <T> ApiResponse<T> serverError(String errorCode, String localizedMessage,
                                                 String path, String traceId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(localizedMessage)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(errorCode)
                .path(path)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse d'erreur 500 Internal Server Error.
     *
     * @param errorCode Code d'erreur technique
     * @param message Message d'erreur localisé
     * @param path Chemin de l'API
     * @param <T> Type de données
     * @return ApiResponse avec statut 500
     */
    public static <T> ApiResponse<T> internalError(String errorCode, String message, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode(errorCode)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ========================================================================
    // SHORTCUTS HTTP (message déjà localisé)
    // ========================================================================

    public static <T> ApiResponse<T> badRequest(String errorCode, String localizedMessage, String path) {
        return error(HttpStatus.BAD_REQUEST, errorCode, localizedMessage, path);
    }

    public static <T> ApiResponse<T> unauthorized(String errorCode, String localizedMessage, String path) {
        return error(HttpStatus.UNAUTHORIZED, errorCode, localizedMessage, path);
    }

    public static <T> ApiResponse<T> forbidden(String errorCode, String localizedMessage, String path) {
        return error(HttpStatus.FORBIDDEN, errorCode, localizedMessage, path);
    }

    public static <T> ApiResponse<T> notFound(String errorCode, String localizedMessage, String path) {
        return error(HttpStatus.NOT_FOUND, errorCode, localizedMessage, path);
    }

    public static <T> ApiResponse<T> internalError(String errorCode, String localizedMessage,
                                                   String path, String traceId) {
        return serverError(errorCode, localizedMessage, path, traceId);
    }

    public static <T> ApiResponse<T> methodNotAllowed(String errorCode, String localizedMessage, String path) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, errorCode, localizedMessage, path);
    }

    public static <T> ApiResponse<T> unsupportedMediaType(String errorCode, String localizedMessage, String path) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, errorCode, localizedMessage, path);
    }

    // ========================================================================
    // MÉTHODES UTILITAIRES
    // ========================================================================

    public boolean hasData() {
        return data != null;
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success) && statusCode != null && statusCode >= 200 && statusCode < 300;
    }

    public boolean isClientError() {
        return Boolean.FALSE.equals(success) && statusCode != null && statusCode >= 400 && statusCode < 500;
    }

    public boolean isServerError() {
        return Boolean.FALSE.equals(success) && statusCode != null && statusCode >= 500;
    }

    public boolean hasValidationErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasTraceId() {
        return traceId != null && !traceId.trim().isEmpty();
    }

    public String getSummary() {
        if (Boolean.TRUE.equals(success)) {
            return String.format("[%d - SUCCESS] %s", statusCode, message);
        } else {
            return String.format("[%d - ERROR] %s (Code: %s)", statusCode, message, errorCode);
        }
    }

    public ApiResponse<T> withPath(String path) {
        this.path = path;
        return this;
    }

    public ApiResponse<T> withData(T data) {
        this.data = data;
        return this;
    }

    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}