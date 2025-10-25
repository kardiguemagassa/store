package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Requête de création d'un Payment Intent Stripe")
public record PaymentIntentRequestDto(

        @NotNull(message = "Le montant est obligatoire")
        @Min(value = 50, message = "Le montant minimum est 50 centimes (0,50€)")
        @Max(value = 99999999, message = "Le montant maximum est 999 999,99€")
        @Schema(
                description = "Montant en centimes (ex: 9999 = 99,99€)",
                example = "9999",
                minimum = "50",
                maximum = "99999999"
        )
        Long amount,

        @NotBlank(message = "La devise est obligatoire")
        @Pattern(
                regexp = "^(eur|usd|gbp|cad)$",
                message = "La devise doit être: eur, usd, gbp ou cad"
        )
        @Schema(
                description = "Code devise ISO 4217 en minuscules",
                example = "eur",
                allowableValues = {"eur", "usd", "gbp", "cad"}
        )
        String currency
) {
        // ✅ Constructeur custom pour normaliser
        public PaymentIntentRequestDto {
                currency = currency != null ? currency.toLowerCase() : null;
        }

        // ✅ Méthode helper
        public BigDecimal getAmountInEuros() {
                return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100));
        }
}