package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * DTO de réponse pour un message de contact.
 *
 * Retourné par l'API pour afficher les messages en administration.
 *
 * @author Kardigué
 * @version 1.0
 */
@Builder
@Schema(description = "Détails d'un message de contact")
public record ContactResponseDto(
        @Schema(description = "Identifiant unique du message", example = "1")
        Long contactId,

        @Schema(description = "Nom du contact", example = "Jean Dupont")
        String name,

        @Schema(description = "Email du contact", example = "jean@example.com")
        String email,

        @Schema(description = "Numéro de mobile", example = "0612345678")
        String mobileNumber,

        @Schema(description = "Contenu du message", example = "Je souhaite des informations...")
        String message,

        @Schema(description = "Statut du message", example = "OPEN", allowableValues = {"OPEN", "IN_PROGRESS", "CLOSED"})
        String status
) {}