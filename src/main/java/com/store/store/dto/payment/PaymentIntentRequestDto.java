package com.store.store.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Requête de création d'un Payment Intent Stripe")
public record PaymentIntentRequestDto(

        @NotNull(message = "{validation.required}")
        @Min(value = 50, message = "{validation.payment.amount.min}")
        @Max(value = 99999999, message = "{validation.payment.amount.max}")
        @Schema(
                description = "Montant en centimes (ex: 9999 = 99,99€)",
                example = "9999",
                minimum = "50",
                maximum = "99999999"
        )
        Long amount,

        @NotBlank(message = "{validation.required}")
        @Pattern(
                regexp = "^(eur|usd|gbp|cad)$",
                message = "{validation.payment.currency.pattern}"
        )
        @Schema(
                description = "Code devise ISO 4217 en minuscules",
                example = "eur",
                allowableValues = {"eur", "usd", "gbp", "cad"}
        )
        String currency
) {

        public PaymentIntentRequestDto {currency = currency != null ? currency.toLowerCase() : currency;}

        // Conversion en euros
        public BigDecimal getAmountInEuros() {return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100));}

        //Conversion en dollars
        public BigDecimal getAmountInDollars() {return getAmountInEuros(); }

        // Vérifie si la devise est supportée
        public boolean isCurrencySupported() {return currency != null && currency.matches("^(eur|usd|gbp|cad)$");}

        // Format montant pour Stripe
        public String getFormattedAmount() {return amount + " " + currency.toUpperCase();}

        //Vérifie le montant minimum selon la devise
        public boolean isAmountValidForCurrency() {
                if (amount == null || currency == null) return false;

                return switch (currency.toLowerCase()) {
                        case "eur" -> amount >= 50;    // 0.50€ minimum
                        case "usd" -> amount >= 50;    // 0.50$ minimum
                        case "gbp" -> amount >= 50;    // 0.50£ minimum
                        case "cad" -> amount >= 50;    // 0.50$ CAD minimum
                        default -> false;
                };
        }

        // création standardisée
        public static PaymentIntentRequestDto of(Long amount, String currency) {
                return new PaymentIntentRequestDto(amount, currency);
        }

        // montant en euros
        public static PaymentIntentRequestDto fromEuros(BigDecimal amountInEuros) {
                Long amountInCents = amountInEuros.multiply(BigDecimal.valueOf(100)).longValue();
                return new PaymentIntentRequestDto(amountInCents, "eur");
        }
}