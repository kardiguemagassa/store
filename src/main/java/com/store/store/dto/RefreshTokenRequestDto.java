// src/main/java/com/store/store/dto/RefreshTokenRequestDto.java

package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

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
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}