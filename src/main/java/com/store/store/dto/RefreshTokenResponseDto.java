// src/main/java/com/store/store/dto/RefreshTokenResponseDto.java

package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO pour la réponse de rafraîchissement du access token.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-01
 */
@Schema(description = "Réponse contenant les nouveaux tokens")
public record RefreshTokenResponseDto(

        @Schema(
                description = "Nouveau JWT access token",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String accessToken,

        @Schema(
                description = "Nouveau refresh token UUID (rotation)",
                example = "abc123-new-token-uuid"
        )
        String refreshToken,

        @Schema(
                description = "Durée de validité du access token en secondes",
                example = "900"
        )
        int expiresIn,

        @Schema(
                description = "Informations de l'utilisateur"
        )
        UserDto user
) {
}