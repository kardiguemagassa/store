package com.store.store.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record OrderRequestDto(
        @NotNull(message = "Le prix total est obligatoire")
        @DecimalMin(value = "0.01", message = "Le prix total doit être supérieur à 0")
        BigDecimal totalPrice,

        @NotBlank(message = "L'ID de paiement est obligatoire")
        String paymentId,

        @NotBlank(message = "Le statut de paiement est obligatoire")
        String paymentStatus,

        @NotNull(message = "La liste des articles est obligatoire")
        @NotEmpty(message = "La commande doit contenir au moins un article")
        @Valid
        List<OrderItemDto> items
) {
}