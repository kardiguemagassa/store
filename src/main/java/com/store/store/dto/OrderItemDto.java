package com.store.store.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record OrderItemDto(
        @NotNull(message = "L'ID du produit est obligatoire")
        @Positive(message = "L'ID du produit doit être positif")
        Long productId,

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 1, message = "La quantité doit être au moins 1")
        Integer quantity,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0")
        BigDecimal price
) {}