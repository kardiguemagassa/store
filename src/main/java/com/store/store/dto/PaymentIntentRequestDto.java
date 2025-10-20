package com.store.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PaymentIntentRequestDto(
        @NotNull(message = "Le montant ne peut pas être null")
        @Min(value = 50, message = "Le montant doit être d'au moins 50 centimes")
        Long amount,

        @NotBlank(message = "La devise ne peut pas être vide")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "La devise doit être un code ISO 4217 de 3 lettres") // Autorise majuscules et minuscules
        String currency
) {}

