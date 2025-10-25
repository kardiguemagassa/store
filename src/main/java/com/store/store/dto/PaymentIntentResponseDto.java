package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant le client secret Stripe")
public record PaymentIntentResponseDto(

        @Schema(
                description = "Client secret du Payment Intent pour finaliser le paiement côté client",
                example = "pi_3ABC123def456GHI_secret_XYZ789uvw012MNO"
        )
        String clientSecret  // ✅ Pas de validation sur les responses
) {}