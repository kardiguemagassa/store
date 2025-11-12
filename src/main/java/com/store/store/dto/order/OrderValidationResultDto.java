package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents the result of an order validation process.
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
        BigDecimal expectedTotal,

        @Schema(
                description = "Indique si les totaux correspondent",
                example = "false"
        )
        Boolean totalsMatch
) {

    /**
     * Constructeur principal avec calcul automatique de totalsMatch
     */
    public OrderValidationResultDto {
        if (totalsMatch == null) {
            totalsMatch = calculateTotalsMatch(calculatedTotal, expectedTotal);
        }
        if (errors == null) {
            errors = List.of();
        }
    }

    /**
     * Constructeur simplifié sans totalsMatch (calculé automatiquement)
     */
    public OrderValidationResultDto(boolean isValid, List<String> errors,
                                    BigDecimal calculatedTotal, BigDecimal expectedTotal) {
        this(isValid, errors, calculatedTotal, expectedTotal, null);
    }

    /**
     * Vérifie s'il y a une discordance entre les totaux
     *
     * @return true si calculatedTotal != expectedTotal
     */
    public boolean hasDiscrepancy() {
        return !totalsMatch;
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
     * Vérifie si la validation a échoué uniquement à cause des totaux
     */
    public boolean isOnlyTotalDiscrepancy() {
        return !isValid &&
                hasDiscrepancy() &&
                (errors == null || errors.size() == 1 && errors.get(0).contains("total"));
    }

    /**
     * Ajoute une erreur et marque comme invalide
     */
    public OrderValidationResultDto withError(String error) {
        List<String> newErrors = errors != null ?
                new java.util.ArrayList<>(errors) : new java.util.ArrayList<>();
        newErrors.add(error);

        return new OrderValidationResultDto(
                false,
                newErrors,
                calculatedTotal,
                expectedTotal,
                totalsMatch
        );
    }

    /**
     * Crée un résultat valide (sans erreurs)
     */
    public static OrderValidationResultDto valid(BigDecimal calculatedTotal, BigDecimal expectedTotal) {
        return new OrderValidationResultDto(
                true,
                List.of(),
                calculatedTotal,
                expectedTotal,
                true
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
                expectedTotal,
                calculateTotalsMatch(calculatedTotal, expectedTotal)
        );
    }

    /**
     * Crée un résultat invalide avec une seule erreur
     */
    public static OrderValidationResultDto invalid(
            String error,
            BigDecimal calculatedTotal,
            BigDecimal expectedTotal
    ) {
        return invalid(List.of(error), calculatedTotal, expectedTotal);
    }

    /**
     * Crée un résultat pour discordance de totaux
     */
    public static OrderValidationResultDto totalDiscrepancy(
            BigDecimal calculatedTotal,
            BigDecimal expectedTotal
    ) {
        return invalid(
                "Discordance entre le total calculé (" + calculatedTotal +
                        ") et le total attendu (" + expectedTotal + ")",
                calculatedTotal,
                expectedTotal
        );
    }

    /**
     * Calcule si les totaux correspondent
     */
    private static boolean calculateTotalsMatch(BigDecimal calculatedTotal, BigDecimal expectedTotal) {
        if (calculatedTotal == null || expectedTotal == null) {
            return false;
        }
        return calculatedTotal.compareTo(expectedTotal) == 0;
    }

    /**
     * Obtient un message de résumé de validation
     */
    public String getSummary() {
        if (isValid) {
            return "La commande est valide";
        }

        if (hasErrors()) {
            return "La commande contient " + getErrorCount() + " erreur(s)";
        }

        return "La commande n'est pas valide";
    }

    /**
     * Vérifie si la commande peut être créée malgré les avertissements
     */
    public boolean canProceedWithWarnings() {
        return !isValid && isOnlyTotalDiscrepancy();
    }
}