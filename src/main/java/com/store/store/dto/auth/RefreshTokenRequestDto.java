package com.store.store.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la requête de rafraîchissement du access token.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-01
 */
@Schema(description = "Requête de rafraîchissement du access token")
public record RefreshTokenRequestDto(

        @Schema(
                description = "Refresh token UUID à échanger",
                example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "{validation.required}")
        @Size(min = 36, max = 36, message = "{validation.refreshToken.size}")
        @Pattern(
                regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
                message = "{validation.refreshToken.pattern}"
        )
        String refreshToken
) {

        /**
         * Factory method pour créer une instance
         */
        public static RefreshTokenRequestDto of(String refreshToken) {
                return new RefreshTokenRequestDto(refreshToken);
        }

        /**
         * Vérifie si le refresh token a un format UUID valide
         */
        public boolean isValidUUIDFormat() {
                return refreshToken != null &&
                        refreshToken.matches("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
        }

        /**
         * Normalise le refresh token (trim et lowercase)
         */
        public String getNormalizedRefreshToken() {
                if (refreshToken == null) {
                        return null;
                }
                return refreshToken.trim().toLowerCase();
        }
}