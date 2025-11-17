package com.store.store.dto.user;

import com.store.store.entity.Customer;
import com.store.store.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Réponse d'opération de promotion/démotion d'utilisateur")
public class PromotionResponseDto {

    @Schema(
            description = "Message décrivant l'opération effectuée",
            example = "Utilisateur promu au rôle Administrateur"
    )
    private String message;

    @Schema(
            description = "Email de l'utilisateur concerné par l'opération",
            example = "jean.dupont@example.com"
    )
    private String userEmail;

    @Schema(
            description = "Nouveau rôle attribué (null en cas de démotion)",
            example = "ADMIN",
            nullable = true
    )
    private String newRole;

    @Schema(
            description = "Email de l'administrateur ayant effectué l'opération",
            example = "admin@store.com"
    )
    private String actionBy;

    @Schema(
            description = "Type d'opération effectuée",
            example = "PROMOTION",
            allowableValues = {"PROMOTION", "DEMOTION", "ROLE_UPDATE"}
    )
    private String actionType;

    @Schema(
            description = "Horodatage de l'opération",
            example = "2024-01-20T14:30:00"
    )
    private LocalDateTime timestamp;

    @Schema(
            description = "Identifiant de l'utilisateur concerné",
            example = "123"
    )
    private Long userId;

    @Schema(
            description = "Nom complet de l'utilisateur concerné",
            example = "Jean Dupont"
    )
    private String userName;

    // FACTORY METHODS

    public static PromotionResponseDto fromPromotion(Customer customer, RoleType newRole, String actionBy) {
        return PromotionResponseDto.builder()
                .message(String.format("Utilisateur promu au rôle %s", newRole.getDisplayName()))
                .userEmail(customer.getEmail())
                .newRole(newRole.name())
                .actionBy(actionBy)
                .actionType("PROMOTION")
                .timestamp(LocalDateTime.now())
                .userId(customer.getCustomerId())
                .userName(customer.getName())
                .build();
    }

    public static PromotionResponseDto fromDemotion(Customer customer, RoleType removedRole, String actionBy) {
        return PromotionResponseDto.builder()
                .message(String.format("Rôle %s retiré avec succès", removedRole.getDisplayName()))
                .userEmail(customer.getEmail())
                .newRole(null)
                .actionBy(actionBy)
                .actionType("DEMOTION")
                .timestamp(LocalDateTime.now())
                .userId(customer.getCustomerId())
                .userName(customer.getName())
                .build();
    }

    public static PromotionResponseDto fromRoleUpdate(Customer customer, RoleType oldRole,
                                                      RoleType newRole, String actionBy) {
        return PromotionResponseDto.builder()
                .message(String.format("Rôle mis à jour de %s à %s",
                        oldRole.getDisplayName(), newRole.getDisplayName()))
                .userEmail(customer.getEmail())
                .newRole(newRole.name())
                .actionBy(actionBy)
                .actionType("ROLE_UPDATE")
                .timestamp(LocalDateTime.now())
                .userId(customer.getCustomerId())
                .userName(customer.getName())
                .build();
    }

    public static PromotionResponseDto fromInitialRole(Customer customer, RoleType initialRole, String actionBy) {
        return PromotionResponseDto.builder()
                .message(String.format("Rôle initial %s attribué", initialRole.getDisplayName()))
                .userEmail(customer.getEmail())
                .newRole(initialRole.name())
                .actionBy(actionBy)
                .actionType("INITIAL_ROLE")
                .timestamp(LocalDateTime.now())
                .userId(customer.getCustomerId())
                .userName(customer.getName())
                .build();
    }


    // MÉTHODES UTILITAIRES

    public boolean isPromotion() {
        return "PROMOTION".equals(actionType);
    }

    public boolean isDemotion() {
        return "DEMOTION".equals(actionType);
    }

    public boolean isRoleUpdate() {
        return "ROLE_UPDATE".equals(actionType);
    }

    public String getActionTypeLabel() {
        if (actionType == null) return "Opération";

        return switch (actionType) {
            case "PROMOTION" -> "Promotion";
            case "DEMOTION" -> "Démotion";
            case "ROLE_UPDATE" -> "Mise à jour de rôle";
            case "INITIAL_ROLE" -> "Attribution de rôle initial";
            default -> actionType;
        };
    }


    public String getOperationSummary() {
        return String.format("%s: %s -> %s (par: %s)",
                getActionTypeLabel(), userEmail, newRole != null ? newRole : "Aucun rôle", actionBy);
    }

    public boolean isRecentOperation() {
        if (timestamp == null) return false;
        return timestamp.isAfter(LocalDateTime.now().minusHours(24));
    }

    public String getMaskedActionBy() {
        if (actionBy == null || actionBy.trim().isEmpty()) {
            return "Système";
        }

        String[] parts = actionBy.split("@");
        if (parts.length != 2) {
            return actionBy;
        }

        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "***@" + domain;
        }

        return username.substring(0, 2) + "***@" + domain;
    }
}