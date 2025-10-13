package com.store.store.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record OrderResponseDto(Long orderId, String status,
                               BigDecimal totalPrice, String createdAt,
                               List<OrderItemReponseDto> items) {
}