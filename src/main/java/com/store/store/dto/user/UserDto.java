package com.store.store.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.store.store.dto.address.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO représentant un utilisateur avec ses informations complètes")
public class UserDto {

    @Schema(
            description = "Identifiant unique de l'utilisateur",
            example = "123"
    )
    private Long userId;

    @Schema(
            description = "Nom complet de l'utilisateur",
            example = "Jean Dupont"
    )
    private String name;

    @Schema(
            description = "Adresse email de l'utilisateur",
            example = "jean.dupont@example.com"
    )
    private String email;

    @Schema(
            description = "Numéro de mobile de l'utilisateur",
            example = "0612345678"
    )
    private String mobileNumber;

    @Schema(
            description = "Rôles de l'utilisateur (séparés par des virgules)",
            example = "ROLE_USER,ROLE_CUSTOMER"
    )
    private String roles;

    @Schema(
            description = "Ensemble des rôles de l'utilisateur",
            example = "[\"ROLE_USER\", \"ROLE_CUSTOMER\"]"
    )
    private Set<String> roleSet;

    @Schema(description = "Adresse de l'utilisateur")
    private AddressDto address;

    @Schema(
            description = "Statut d'activation du compte",
            example = "true"
    )
    private Boolean isActive;

    @Schema(
            description = "Date de création du compte",
            example = "2024-01-15T10:30:00"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(
            description = "Date de dernière mise à jour",
            example = "2024-01-20T14:25:00"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(
            description = "Date de dernière connexion",
            example = "2024-01-20T14:20:00"
    )
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @Schema(
            description = "URL de l'avatar de l'utilisateur",
            example = "/images/avatars/user123.jpg"
    )
    private String avatarUrl;

    // MÉTHODES UTILITAIRES

    public boolean hasRole(String role) {
        if (roles == null && roleSet == null) {
            return false;
        }

        if (roleSet != null) {
            return roleSet.contains(role);
        }

        // Fallback pour l'ancien format string
        return roles != null && roles.contains(role);
    }


    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN") || hasRole("ADMIN");
    }


    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }


    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (email != null) {
            return email.split("@")[0]; // Partie avant @
        }
        return "Utilisateur";
    }


    public String getMaskedEmail() {
        if (email == null || email.trim().isEmpty()) {
            return "";
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }

        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "***@" + domain;
        }

        return username.substring(0, 2) + "***@" + domain;
    }


    public String getMaskedMobileNumber() {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            return "";
        }

        String cleanNumber = mobileNumber.replaceAll("\\s+", "");
        if (cleanNumber.length() <= 4) {
            return "****";
        }

        return "******" + cleanNumber.substring(cleanNumber.length() - 4);
    }


    public String getPrimaryRole() {
        if (roleSet != null && !roleSet.isEmpty()) {
            // Priorité aux rôles admin
            if (roleSet.contains("ROLE_ADMIN")) return "ROLE_ADMIN";
            if (roleSet.contains("ROLE_MANAGER")) return "ROLE_MANAGER";
            if (roleSet.contains("ROLE_EMPLOYEE")) return "ROLE_EMPLOYEE";
            return roleSet.iterator().next();
        }

        if (roles != null && !roles.trim().isEmpty()) {
            String[] roleArray = roles.split(",");
            if (roleArray.length > 0) {
                return roleArray[0].trim();
            }
        }

        return "ROLE_USER";
    }


    public boolean isRecentlyActive() {
        if (lastLoginAt == null) {
            return false;
        }
        return lastLoginAt.isAfter(LocalDateTime.now().minusDays(7));
    }


    public static UserDto basic(Long userId, String name, String email) {
        return UserDto.builder()
                .userId(userId)
                .name(name)
                .email(email)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    public static UserDto publicView(Long userId, String name) {
        return UserDto.builder()
                .userId(userId)
                .name(name)
                // Email et mobileNumber non inclus pour la confidentialité
                .isActive(true)
                .build();
    }


    public static UserDto withRoles(Long userId, String name, String email, Set<String> roles) {
        return UserDto.builder()
                .userId(userId)
                .name(name)
                .email(email)
                .roleSet(roles)
                .roles(String.join(",", roles))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}