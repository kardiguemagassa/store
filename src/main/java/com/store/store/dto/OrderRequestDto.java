package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Requête de création d'une commande")
public class OrderRequestDto {

        @NotNull(message = "Le prix total est obligatoire")
        @DecimalMin(value = "0.01", message = "Le prix total doit être supérieur à 0")
        @DecimalMax(value = "99999.99", message = "Le prix maximum est 99 999,99€")
        @Schema(description = "Montant total de la commande en euros", example = "99.99")
        private BigDecimal totalPrice;

        @NotBlank(message = "L'ID du paiement Stripe est obligatoire")
        @Size(max = 250, message = "L'ID du paiement ne peut pas dépasser 250 caractères")
        @Pattern(
                regexp = "^pi_[a-zA-Z0-9]{24,}$",
                message = "L'ID du paiement doit être un Payment Intent Stripe valide (format: pi_...)"
        )
        @Schema(
                description = "Payment Intent ID de Stripe",
                example = "pi_3SLqXpDWbglQHB6C077zyM50",
                pattern = "^pi_[a-zA-Z0-9]{24,}$"
        )
        private String paymentIntentId;

        @NotBlank(message = "Le statut de paiement est obligatoire")
        @Schema(
                description = "Statut du paiement Stripe (sera normalisé en interne)",
                example = "succeeded",
                allowableValues = {
                        "succeeded",              // Paiement réussi
                        "processing",             // En cours de traitement
                        "requires_payment_method", // Méthode de paiement requise
                        "requires_confirmation",  // Confirmation requise
                        "requires_action",        // Action utilisateur requise (3D Secure)
                        "canceled",               // Paiement annulé
                        "failed",                 // Paiement échoué
                        "paid",                   // Format interne (rétrocompatibilité)
                        "pending"                 // Format interne (rétrocompatibilité)
                }
        )
        private String paymentStatus;

        @NotNull(message = "La liste des articles est obligatoire")
        @NotEmpty(message = "La commande doit contenir au moins un article")
        @Size(max = 50, message = "Une commande ne peut pas contenir plus de 50 articles")
        @Valid
        @Schema(description = "Liste des articles de la commande")
        private List<OrderItemDto> items;


        /**
         *  Normalise le statut de paiement Stripe vers notre format interne
         *
         * Mapping:
         * - "succeeded" → "paid"
         * - "processing", "requires_*" → "pending"
         * - "canceled", "failed" → "failed"
         *
         * @return Le statut normalisé (paid, pending, ou failed)
         */
        public String getNormalizedPaymentStatus() {
                if (paymentStatus == null || paymentStatus.isBlank()) {
                        return "failed";
                }

                return switch (paymentStatus.toLowerCase().trim()) {
                        case "succeeded" -> "paid";
                        case "processing",
                             "requires_payment_method",
                             "requires_confirmation",
                             "requires_action" -> "pending";
                        case "canceled", "failed" -> "failed";
                        case "paid", "pending" -> paymentStatus.toLowerCase(); // Déjà normalisé
                        default -> {
                                // Log pour les statuts inconnus
                                System.err.println("Unknown payment status: " + paymentStatus + ", defaulting to pending");
                                yield "pending";
                        }
                };
        }

        /**
         * ✅ Calcule le total attendu basé sur les items
         * Utile pour validation de cohérence
         *
         * @return Le montant total calculé
         */
        public BigDecimal calculateExpectedTotal() {
                if (items == null || items.isEmpty()) {
                        return BigDecimal.ZERO;
                }
                return items.stream()
                        .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        /**
         * ✅ Vérifie si le paiement est réussi
         *
         * @return true si le paiement est succeeded ou paid
         */
        public boolean isPaymentSuccessful() {
                if (paymentStatus == null) {
                        return false;
                }
                String normalized = paymentStatus.toLowerCase().trim();
                return "succeeded".equals(normalized) || "paid".equals(normalized);
        }

        /**
         * ✅ Vérifie si le total fourni correspond au total calculé
         *
         * @return true si les totaux correspondent
         */
        public boolean isTotalValid() {
                if (totalPrice == null) {
                        return false;
                }
                BigDecimal expected = calculateExpectedTotal();
                return totalPrice.compareTo(expected) == 0;
        }

        /**
         * ✅ Compte le nombre total d'items
         *
         * @return Le nombre d'items dans la commande
         */
        public int getTotalItemCount() {
                if (items == null) {
                        return 0;
                }
                return items.stream()
                        .mapToInt(OrderItemDto::quantity)
                        .sum();
        }


}