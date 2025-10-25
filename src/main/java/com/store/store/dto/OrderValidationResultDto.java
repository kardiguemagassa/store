package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * ✅ Résultat de validation métier d'une commande
 *
 * Utilisé pour :
 * - Validation pré-création (endpoint /orders/validate)
 * - Vérification de cohérence des données
 * - Détection de discordances (total calculé vs total attendu)
 * - Feedback détaillé au frontend
 *
 * Exemple de réponse :
 * {
 *   "isValid": false,
 *   "errors": [
 *     "Total incohérent",
 *     "Stock insuffisant pour le produit 5"
 *   ],
 *   "calculatedTotal": 99.98,
 *   "expectedTotal": 100.00
 * }
 */
@Schema(description = "Résultat de validation d'une commande")
public record OrderValidationResultDto(

        @Schema(
                description = "Indique si la commande est valide",
                example = "false"
        )
        boolean isValid,

        @Schema(
                description = "Liste des erreurs de validation métier",
                example = "[\"Total incohérent\", \"Stock insuffisant pour le produit 5\"]"
        )
        List<String> errors,

        @Schema(
                description = "Total calculé à partir des items",
                example = "99.98"
        )
        BigDecimal calculatedTotal,

        @Schema(
                description = "Total attendu fourni dans la requête",
                example = "100.00"
        )
        BigDecimal expectedTotal
) {
    /**
     * Vérifie s'il y a une discordance entre les totaux
     *
     * @return true si calculatedTotal != expectedTotal
     */
    public boolean hasDiscrepancy() {
        if (calculatedTotal == null || expectedTotal == null) {
            return false;
        }
        return calculatedTotal.compareTo(expectedTotal) != 0;
    }

    /**
     * Calcule la différence entre les totaux
     *
     * @return calculatedTotal - expectedTotal
     */
    public BigDecimal getDiscrepancy() {
        if (calculatedTotal == null || expectedTotal == null) {
            return BigDecimal.ZERO;
        }
        return calculatedTotal.subtract(expectedTotal);
    }

    /**
     * Retourne le nombre d'erreurs
     */
    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    /**
     * Vérifie si des erreurs existent
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Crée un résultat valide (sans erreurs)
     */
    public static OrderValidationResultDto valid(BigDecimal calculatedTotal, BigDecimal expectedTotal) {
        return new OrderValidationResultDto(
                true,
                List.of(),
                calculatedTotal,
                expectedTotal
        );
    }

    /**
     * Crée un résultat invalide avec erreurs
     */
    public static OrderValidationResultDto invalid(
            List<String> errors,
            BigDecimal calculatedTotal,
            BigDecimal expectedTotal
    ) {
        return new OrderValidationResultDto(
                false,
                errors,
                calculatedTotal,
                expectedTotal
        );
    }
}