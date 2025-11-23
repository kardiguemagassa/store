package com.store.store.dto.order;

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

    public OrderValidationResultDto {
        if (totalsMatch == null) {
            totalsMatch = calculateTotalsMatch(calculatedTotal, expectedTotal);
        }
        if (errors == null) {
            errors = List.of();
        }
    }


    public OrderValidationResultDto(boolean isValid, List<String> errors,
                                    BigDecimal calculatedTotal, BigDecimal expectedTotal) {
        this(isValid, errors, calculatedTotal, expectedTotal, null);
    }

    public boolean hasDiscrepancy() {
        return !totalsMatch;
    }

    public BigDecimal getDiscrepancy() {
        if (calculatedTotal == null || expectedTotal == null) {
            return BigDecimal.ZERO;
        }
        return calculatedTotal.subtract(expectedTotal);
    }

    public int getErrorCount() {
        return errors != null ? errors.size() : 0;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean isOnlyTotalDiscrepancy() {
        return !isValid &&
                hasDiscrepancy() &&
                (errors == null || errors.size() == 1 && errors.getFirst().contains("total"));
    }


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

    public static OrderValidationResultDto valid(BigDecimal calculatedTotal, BigDecimal expectedTotal) {
        return new OrderValidationResultDto(
                true,
                List.of(),
                calculatedTotal,
                expectedTotal,
                Boolean.TRUE
        );
    }

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

    public static OrderValidationResultDto invalid(
            String error,
            BigDecimal calculatedTotal,
            BigDecimal expectedTotal
    ) {
        return invalid(List.of(error), calculatedTotal, expectedTotal);
    }

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

    // CORRECTION: Modifier le type de retour de la méthode
    private static Boolean calculateTotalsMatch(BigDecimal calculatedTotal, BigDecimal expectedTotal) {
        if (calculatedTotal == null || expectedTotal == null) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(calculatedTotal.compareTo(expectedTotal) == 0);
    }

    public String getSummary() {
        if (isValid) {
            return "La commande est valide";
        }

        if (hasErrors()) {
            return "La commande contient " + getErrorCount() + " erreur(s)";
        }

        return "La commande n'est pas valide";
    }

    public boolean canProceedWithWarnings() {
        return !isValid && isOnlyTotalDiscrepancy();
    }
}