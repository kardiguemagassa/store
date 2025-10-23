package com.store.store.dto;

import com.store.store.entity.Customer;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PromotionResponseDto {
    private String message;
    private String userEmail;
    private String promotedBy;
    private LocalDateTime timestamp;

    public static PromotionResponseDto from(Customer customer, String promotedBy) {
        return PromotionResponseDto.builder()
                .message("Utilisateur promu avec succès")
                .userEmail(customer.getEmail())
                .promotedBy(promotedBy)
                .timestamp(LocalDateTime.now())
                .build();
    }
}