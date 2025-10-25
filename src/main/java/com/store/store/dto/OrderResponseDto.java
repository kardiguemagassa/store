package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Détails d'une commande")
public class OrderResponseDto {

    @Schema(description = "Identifiant unique de la commande", example = "1")
    private Long orderId;

    @Schema(description = "Statut de la commande", example = "CREATED", allowableValues = {"CREATED", "CONFIRMED", "CANCELLED", "DELIVERED"})
    private String orderStatus;

    @Schema(description = "Montant total de la commande", example = "99.99")
    private BigDecimal totalPrice;

    @Schema(description = "Payment Intent ID de Stripe", example = "pi_3ABC123def456GHI")
    private String paymentIntentId;

    @Schema(description = "Statut du paiement", example = "paid", allowableValues = {"paid", "pending", "failed"})
    private String paymentStatus;

    @Schema(description = "Date de création de la commande", example = "2025-10-24T14:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Date de dernière modification", example = "2025-10-24T15:45:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Articles de la commande")
    private List<OrderItemResponseDto> items;

    // ✅ Méthode helper
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }

    // ✅ Méthode helper
    public boolean isPaid() {
        return "paid".equalsIgnoreCase(paymentStatus);
    }
}