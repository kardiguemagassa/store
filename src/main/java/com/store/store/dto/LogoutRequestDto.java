package com.store.store.dto;


import jakarta.validation.constraints.NotBlank;

/**
 * @param refreshToken Le refresh token à révoquer
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-01
 */
public record LogoutRequestDto(

        /**
         * Le refresh token à révoquer lors du logout.
         *
         * <p>Format : UUID v4 (36 caractères)</p>
         *
         * <p><strong>Comportement :</strong></p>
         * <ul>
         *   <li>Si le token existe → Révoqué (revoked = true)</li>
         *   <li>Si le token n'existe pas → Aucune erreur (idempotent)</li>
         *   <li>Si le token est déjà révoqué → Aucune erreur (idempotent)</li>
         * </ul>
         */
        @NotBlank(message = "Refresh token is required for logout")
        String refreshToken
) {
}
