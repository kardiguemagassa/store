package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Réponse contenant le client secret Stripe")
public record PaymentIntentResponseDto(

        @Schema(
                description = "Client secret du Payment Intent pour finaliser le paiement côté client",
                example = "pi_3ABC123def456GHI_secret_XYZ789uvw012MNO"
        )
        String clientSecret,

        @Schema(
                description = "ID du Payment Intent Stripe",
                example = "pi_3ABC123def456GHI"
        )
        String paymentIntentId,

        @Schema(
                description = "Statut du Payment Intent",
                example = "requires_payment_method",
                allowableValues = {
                        "requires_payment_method", "requires_confirmation", "requires_action",
                        "processing", "succeeded", "canceled"
                }
        )
        String status,

        @Schema(
                description = "Montant du paiement en centimes",
                example = "9999"
        )
        Long amount,

        @Schema(
                description = "Devise du paiement",
                example = "eur"
        )
        String currency
) {

        // ✅ Factory method depuis un Payment Intent Stripe
        public static PaymentIntentResponseDto fromStripeIntent(com.stripe.model.PaymentIntent stripeIntent) {
                return new PaymentIntentResponseDto(
                        stripeIntent.getClientSecret(),
                        stripeIntent.getId(),
                        stripeIntent.getStatus(),
                        stripeIntent.getAmount(),
                        stripeIntent.getCurrency()
                );
        }

        // ✅ Factory method simplifiée (pour tests ou cas simples)
        public static PaymentIntentResponseDto of(String clientSecret, String paymentIntentId) {
                return new PaymentIntentResponseDto(
                        clientSecret,
                        paymentIntentId,
                        "requires_payment_method",
                        null,
                        null
                );
        }

        // ✅ Méthode helper - Vérifie si le paiement nécessite une action
        public boolean requiresAction() {
                return "requires_action".equals(status) || "requires_payment_method".equals(status);
        }

        // ✅ Méthode helper - Vérifie si le paiement est réussi
        public boolean isSucceeded() {
                return "succeeded".equals(status);
        }

        // ✅ Méthode helper - Vérifie si le paiement est en cours
        public boolean isProcessing() {
                return "processing".equals(status);
        }

        // ✅ Méthode helper - Vérifie si le paiement est annulé
        public boolean isCanceled() {
                return "canceled".equals(status);
        }

        // ✅ Méthode helper - Conversion du montant en euros
        public java.math.BigDecimal getAmountInEuros() {
                if (amount == null) return null;
                return java.math.BigDecimal.valueOf(amount).divide(java.math.BigDecimal.valueOf(100));
        }

        // ✅ Méthode helper - Obtient le statut lisible
        public String getStatusLabel() {
                if (status == null) return "Inconnu";

                return switch (status) {
                        case "requires_payment_method" -> "Méthode de paiement requise";
                        case "requires_confirmation" -> "Confirmation requise";
                        case "requires_action" -> "Action requise";
                        case "processing" -> "En traitement";
                        case "succeeded" -> "Réussi";
                        case "canceled" -> "Annulé";
                        default -> status;
                };
        }
}