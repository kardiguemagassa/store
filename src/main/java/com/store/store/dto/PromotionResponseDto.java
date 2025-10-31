package com.store.store.dto;

import com.store.store.entity.Customer;
import com.store.store.enums.RoleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PromotionResponseDto {
    private String message;
    private String userEmail;
    private String newRole;  // ✅ String pour le nom du rôle
    private String promotedBy;
    private LocalDateTime timestamp;

    // ✅ Méthode factory avec le nouveau rôle
    public static PromotionResponseDto from(Customer customer, RoleType newRole, String promotedBy) {
        return PromotionResponseDto.builder()
                .message("Utilisateur promu au rôle " + newRole.getDisplayName())
                .userEmail(customer.getEmail())
                .newRole(newRole.getDisplayName())  // ✅ Conversion en String
                .promotedBy(promotedBy)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ✅ Méthode alternative pour la rétrogradation
    public static PromotionResponseDto demotion(Customer customer, RoleType removedRole, String demotedBy) {
        return PromotionResponseDto.builder()
                .message("Rôle " + removedRole.getDisplayName() + " retiré avec succès")
                .userEmail(customer.getEmail())
                .newRole(null)  // Pas de nouveau rôle dans ce cas
                .promotedBy(demotedBy)
                .timestamp(LocalDateTime.now())
                .build();
    }
}