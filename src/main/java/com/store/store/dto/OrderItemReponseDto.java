package com.store.store.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItemReponseDto(String productName, Integer quantity,
                                  BigDecimal price, String imageUrl) {
}
