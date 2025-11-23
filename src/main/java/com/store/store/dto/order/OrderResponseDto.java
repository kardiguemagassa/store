package com.store.store.dto.order;

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

    // AJOUT: Informations client
    @Schema(description = "Email du client", example = "client@example.com")
    private String customerEmail;

    @Schema(description = "Nom du client", example = "John Doe")
    private String customerName;

    @Schema(description = "Articles de la commande")
    private List<OrderItemResponseDto> items;

    // Méthode helper - Nombre total d'articles
    public int getTotalItems() {
        return items != null ? items.size() : 0;
    }

    // Méthode helper - Vérifie si le paiement est effectué
    public boolean isPaid() {
        return "paid".equalsIgnoreCase(paymentStatus);
    }

    // Méthode helper - Vérifie si la commande est annulée
    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(orderStatus);
    }

    // Méthode helper - Vérifie si la commande est livrée
    public boolean isDelivered() {
        return "DELIVERED".equalsIgnoreCase(orderStatus);
    }

    // Méthode helper - Calcule la quantité totale d'articles
    public int getTotalItemQuantity() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream().mapToInt(OrderItemResponseDto::getQuantity).sum();
    }

    // Méthode helper - Vérifie si la commande peut être modifiée
    public boolean isEditable() {
        return !isCancelled() && !isDelivered() && !isPaid();
    }

    // Factory method avec informations client
    public static OrderResponseDto of(Long orderId, String orderStatus, BigDecimal totalPrice,
                                      String paymentIntentId, String paymentStatus,
                                      String customerEmail, String customerName,
                                      List<OrderItemResponseDto> items) {
        return OrderResponseDto.builder()
                .orderId(orderId)
                .orderStatus(orderStatus)
                .totalPrice(totalPrice)
                .paymentIntentId(paymentIntentId)
                .paymentStatus(paymentStatus)
                .customerEmail(customerEmail)
                .customerName(customerName)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(items)
                .build();
    }

    // Factory method pour commande créée avec client
    public static OrderResponseDto created(Long orderId, BigDecimal totalPrice,
                                           String paymentIntentId, String customerEmail, String customerName,
                                           List<OrderItemResponseDto> items) {
        return OrderResponseDto.builder()
                .orderId(orderId)
                .orderStatus("CREATED")
                .totalPrice(totalPrice)
                .paymentIntentId(paymentIntentId)
                .paymentStatus("pending")
                .customerEmail(customerEmail)
                .customerName(customerName)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(items)
                .build();
    }

    // Méthode pour obtenir le statut lisible
    public String getOrderStatusLabel() {
        if (orderStatus == null) return "Inconnu";

        return switch (orderStatus.toUpperCase()) {
            case "CREATED" -> "Créée";
            case "CONFIRMED" -> "Confirmée";
            case "CANCELLED" -> "Annulée";
            case "DELIVERED" -> "Livrée";
            default -> orderStatus;
        };
    }

    // Méthode pour obtenir le statut de paiement lisible
    public String getPaymentStatusLabel() {
        if (paymentStatus == null) return "Inconnu";

        return switch (paymentStatus.toLowerCase()) {
            case "paid" -> "Payé";
            case "pending" -> "En attente";
            case "failed" -> "Échoué";
            default -> paymentStatus;
        };
    }

    // obtenir l'affichage client
    public String getCustomerDisplay() {
        if (customerName != null && customerEmail != null) {
            return customerName + " (" + customerEmail + ")";
        } else if (customerEmail != null) {
            return customerEmail;
        } else if (customerName != null) {
            return customerName;
        }
        return "Client inconnu";
    }
}