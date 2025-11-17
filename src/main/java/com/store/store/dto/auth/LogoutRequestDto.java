package com.store.store.dto.auth;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Représente un objet de transfert de données (DTO) utilisé pour les demandes de déconnexion.
 * Cet enregistrement encapsule les données nécessaires à l'exécution d'une opération de déconnexion.
 * @author Kardigué
 * @version 1.0
 * @since 2025-11-01
 */
public record LogoutRequestDto(

        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
                message = "{validation.logout.refreshToken.pattern}")
        String refreshToken
) {
}
