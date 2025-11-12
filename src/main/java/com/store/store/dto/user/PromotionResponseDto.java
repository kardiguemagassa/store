package com.store.store.dto;

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

    // ========================================================================
    // FACTORY METHODS
    // ========================================================================

    /**
     * Crée une réponse pour une promotion
     */
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

    /**
     * Crée une réponse pour une démotion
     */
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

    /**
     * Crée une réponse pour une mise à jour de rôle
     */
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

    /**
     * Crée une réponse pour l'attribution d'un rôle initial
     */
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

    // ========================================================================
    // MÉTHODES UTILITAIRES
    // ========================================================================

    /**
     * Vérifie si l'opération est une promotion
     */
    public boolean isPromotion() {
        return "PROMOTION".equals(actionType);
    }

    /**
     * Vérifie si l'opération est une démotion
     */
    public boolean isDemotion() {
        return "DEMOTION".equals(actionType);
    }

    /**
     * Vérifie si l'opération est une mise à jour de rôle
     */
    public boolean isRoleUpdate() {
        return "ROLE_UPDATE".equals(actionType);
    }

    /**
     * Retourne le type d'action sous forme lisible
     */
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

    /**
     * Retourne un résumé de l'opération pour les logs
     */
    public String getOperationSummary() {
        return String.format("%s: %s -> %s (par: %s)",
                getActionTypeLabel(), userEmail, newRole != null ? newRole : "Aucun rôle", actionBy);
    }

    /**
     * Vérifie si l'opération a été effectuée récemment
     */
    public boolean isRecentOperation() {
        if (timestamp == null) return false;
        return timestamp.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Masque partiellement l'email de l'actionnaire pour la confidentialité
     */
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