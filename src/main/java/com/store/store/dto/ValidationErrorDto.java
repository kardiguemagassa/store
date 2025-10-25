package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO pour les erreurs de validation
 *
 * Utilisé dans GlobalExceptionHandler pour standardiser
 * le format des erreurs de validation Jakarta Bean Validation
 *
 * Exemple de réponse :
 * {
 *   "field": "totalPrice",
 *   "message": "Le prix total doit être positif",
 *   "rejectedValue": -10.50
 * }
 */
@Schema(description = "Détails d'une erreur de validation")
public record ValidationErrorDto(

        @Schema(
                description = "Nom du champ en erreur",
                example = "totalPrice"
        )
        String field,

        @Schema(
                description = "Message d'erreur descriptif",
                example = "Le prix total doit être supérieur à 0"
        )
        String message,

        @Schema(
                description = "Valeur rejetée par la validation",
                example = "-10.50",
                nullable = true
        )
        Object rejectedValue
) {
        /**
         * Constructeur sans rejectedValue (pour erreurs sans valeur)
         */
        public ValidationErrorDto(String field, String message) {
                this(field, message, null);
        }

        /**
         * Vérifie si une valeur a été rejetée
         */
        public boolean hasRejectedValue() {
                return rejectedValue != null;
        }

        /**
         * Retourne la valeur rejetée sous forme de String pour affichage
         */
        public String getRejectedValueAsString() {
                return rejectedValue != null ? rejectedValue.toString() : "null";
        }
}