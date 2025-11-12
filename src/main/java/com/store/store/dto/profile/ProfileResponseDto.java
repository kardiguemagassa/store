package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@Schema(description = "Réponse du profil utilisateur")
public class ProfileResponseDto {

    @Schema(description = "Identifiant unique du client", example = "123")
    private Long customerId;

    @Schema(description = "Nom complet du client", example = "Jean Dupont")
    private String name;

    @Schema(description = "Adresse email du client", example = "jean.dupont@example.com")
    private String email;

    @Schema(description = "Numéro de mobile du client", example = "0612345678")
    private String mobileNumber;

    @Schema(description = "Adresse du client")
    private AddressDto address;

    @Schema(description = "Indique si l'email a été mis à jour récemment", example = "false")
    private boolean emailUpdated;

    @Schema(description = "Date de création du profil", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Date de dernière mise à jour du profil", example = "2024-01-20T14:25:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Statut d'activation du compte", example = "true")
    private Boolean isActive;


    public ProfileResponseDto() {

    }

    public ProfileResponseDto(Long customerId, String name, String email, String mobileNumber,
                              AddressDto address, boolean emailUpdated) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.address = address;
        this.emailUpdated = emailUpdated;
    }

    // MÉTHODES UTILITAIRES
    /**
     * Vérifie si le profil est actif
     */
    public boolean isProfileActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * Retourne le nom formaté pour l'affichage
     */
    public String getDisplayName() {
        if (name == null || name.trim().isEmpty()) {
            return "Utilisateur";
        }
        return name.trim();
    }

    /**
     * Masque partiellement l'email pour la confidentialité
     */
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

    /**
     * Masque partiellement le numéro de mobile pour la confidentialité
     */
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

    /**
     * Vérifie si le profil a été récemment mis à jour
     */
    public boolean isRecentlyUpdated() {
        if (updatedAt == null) {
            return false;
        }
        return updatedAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    // FACTORY METHODS
    /**
     * Crée une réponse de profil basique
     */
    public static ProfileResponseDto basic(Long customerId, String name, String email, String mobileNumber) {
        return ProfileResponseDto.builder()
                .customerId(customerId)
                .name(name)
                .email(email)
                .mobileNumber(mobileNumber)
                .emailUpdated(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse de profil complète
     */
    public static ProfileResponseDto complete(Long customerId, String name, String email,
                                              String mobileNumber, AddressDto address) {
        return ProfileResponseDto.builder()
                .customerId(customerId)
                .name(name)
                .email(email)
                .mobileNumber(mobileNumber)
                .address(address)
                .emailUpdated(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Crée une réponse pour une mise à jour d'email
     */
    public static ProfileResponseDto withEmailUpdated(Long customerId, String name, String newEmail,
                                                      String mobileNumber, AddressDto address) {
        return ProfileResponseDto.builder()
                .customerId(customerId)
                .name(name)
                .email(newEmail)
                .mobileNumber(mobileNumber)
                .address(address)
                .emailUpdated(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}