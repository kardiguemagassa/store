package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Article d'une commande")
public record OrderItemDto(

        @NotNull(message = "L'ID du produit est obligatoire")
        @Positive(message = "L'ID du produit doit être positif")
        @Schema(description = "Identifiant du produit", example = "42")
        Long productId,

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 1, message = "La quantité minimum est 1")
        @Max(value = 999, message = "La quantité maximum est 999")
        @Schema(description = "Quantité commandée", example = "2", minimum = "1", maximum = "999")
        Integer quantity,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0")
        @DecimalMax(value = "99999.99", message = "Le prix ne peut pas dépasser 99 999,99€")
        @Schema(description = "Prix unitaire du produit", example = "49.99")
        BigDecimal price
) {
        // ✅ Méthode helper
        public BigDecimal getSubtotal() {
                return price.multiply(BigDecimal.valueOf(quantity));
        }
}