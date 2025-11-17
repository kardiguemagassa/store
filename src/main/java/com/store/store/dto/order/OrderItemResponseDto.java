package com.store.store.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Article d'une commande")
public class OrderItemResponseDto {

    @Schema(description = "Identifiant de l'article", example = "1")
    private Long orderItemId;

    @Schema(description = "Identifiant du produit", example = "42")
    private Long productId;

    @Schema(description = "Nom du produit", example = "Sticker Dragon Ball Z")
    private String productName;

    @Schema(description = "URL de l'image du produit", example = "/images/products/dbz-sticker.jpg")
    private String productImageUrl;

    @Schema(description = "Quantité commandée", example = "2")
    private Integer quantity;

    @Schema(description = "Prix unitaire", example = "49.99")
    private BigDecimal price;

    @Schema(description = "Prix total (prix × quantité)", example = "99.98")
    private BigDecimal subtotal;

    //Factory method
    public static OrderItemResponseDto of(Long orderItemId, Long productId, String productName,
                                          String productImageUrl, Integer quantity, BigDecimal price) {
        BigDecimal calculatedSubtotal = price.multiply(BigDecimal.valueOf(quantity));

        return OrderItemResponseDto.builder()
                .orderItemId(orderItemId)
                .productId(productId)
                .productName(productName)
                .productImageUrl(productImageUrl)
                .quantity(quantity)
                .price(price)
                .subtotal(calculatedSubtotal)
                .build();
    }
}