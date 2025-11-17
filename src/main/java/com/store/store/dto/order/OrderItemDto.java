package com.store.store.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Article d'une commande")
public record OrderItemDto(

        @NotNull(message = "{validation.required}")
        @Positive(message = "{validation.positive}")
        @Schema(description = "Identifiant du produit", example = "42")
        Long productId,

        @NotNull(message = "{validation.required}")
        @Min(value = 1, message = "{validation.min.value}")
        @Max(value = 999, message = "{validation.max.value}")
        @Schema(description = "Quantité commandée", example = "2", minimum = "1", maximum = "999")
        Integer quantity,

        @NotNull(message = "{validation.required}")
        @DecimalMin(value = "0.01", message = "{validation.decimal.min}")
        @DecimalMax(value = "99999.99", message = "{validation.decimal.max}")
        @Digits(integer = 5, fraction = 2, message = "{validation.digits}")
        @Schema(description = "Prix unitaire du produit", example = "49.99")
        BigDecimal price

) {     //Méthode helper
        public BigDecimal getSubtotal() {return price.multiply(BigDecimal.valueOf(quantity));}
}