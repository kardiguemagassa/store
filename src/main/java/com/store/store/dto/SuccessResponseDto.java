package com.store.store.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ DTO de réponse de succès standardisé
 *
 * Utilisé pour toutes les réponses API réussies
 *
 * Exemple de réponse :
 * {
 *   "success": true,
 *   "message": "Commande créée avec succès",
 *   "status": 201,
 *   "timestamp": "2025-10-24T21:30:00",
 *   "data": { "orderId": 123 }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Réponse de succès standardisée de l'API")
public class SuccessResponseDto {

    @Schema(
            description = "Indicateur de succès",
            example = "true"
    )
    @Builder.Default
    private Boolean success = true;  // ✅ Ajout du champ success

    @Schema(
            description = "Message de succès",
            example = "Commande créée avec succès"
    )
    private String message;

    @Schema(
            description = "Code de statut HTTP",
            example = "200"
    )
    private Integer status;

    @Schema(
            description = "Horodatage de la réponse",
            example = "2025-10-24T21:30:00"
    )
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(
            description = "Données additionnelles (optionnel)",
            nullable = true
    )
    private Object data;

    /**
     * Crée une réponse de succès simple (sans data)
     */
    public static SuccessResponseDto of(String message, int status) {
        return SuccessResponseDto.builder()
                .success(true)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse de succès avec data
     */
    public static SuccessResponseDto of(String message, int status, Object data) {
        return SuccessResponseDto.builder()
                .success(true)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    /**
     * Crée une réponse de succès simple (200 OK)
     */
    public static SuccessResponseDto ok(String message) {
        return SuccessResponseDto.builder()
                .success(true)
                .message(message)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse de succès avec data (200 OK)
     */
    public static SuccessResponseDto ok(String message, Object data) {
        return SuccessResponseDto.builder()
                .success(true)
                .message(message)
                .status(200)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    /**
     * Crée une réponse de création réussie (201 CREATED)
     */
    public static SuccessResponseDto created(String message) {
        return SuccessResponseDto.builder()
                .success(true)
                .message(message)
                .status(201)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse de création réussie avec data (201 CREATED)
     */
    public static SuccessResponseDto created(String message, Object data) {
        return SuccessResponseDto.builder()
                .success(true)
                .message(message)
                .status(201)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }
}