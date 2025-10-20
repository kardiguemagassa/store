package com.store.store.dto;

import jakarta.validation.constraints.NotBlank;


public record PaymentIntentResponseDto(
        @NotBlank(message = "Le client secret ne peut pas être vide")
        String clientSecret
) {}