package com.store.store.dto;

import com.store.store.dto.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse de rafraîchissement du access token.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-11-01
 */
@Builder
@Schema(description = "Réponse contenant les nouveaux tokens")
public record RefreshTokenResponseDto(

        @Schema(
                description = "Nouveau JWT access token",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String accessToken,

        @Schema(
                description = "Type de token",
                example = "Bearer",
                defaultValue = "Bearer"
        )
        String tokenType,

        @Schema(
                description = "Nouveau refresh token UUID (rotation optionnelle)",
                example = "550e8400-e29b-41d4-a716-446655440001",
                nullable = true
        )
        String refreshToken,

        @Schema(
                description = "Durée de validité du access token en secondes",
                example = "900",
                defaultValue = "900"
        )
        Integer expiresIn,

        @Schema(
                description = "Date d'expiration du access token",
                example = "2024-01-20T15:30:00"
        )
        LocalDateTime accessTokenExpiry,

        @Schema(
                description = "Date d'expiration du refresh token",
                example = "2024-02-20T14:30:00"
        )
        LocalDateTime refreshTokenExpiry,

        @Schema(
                description = "Informations de l'utilisateur",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UserDto user
) {

        // ========================================================================
        // FACTORY METHODS
        // ========================================================================

        /**
         * Crée une réponse avec rotation du refresh token
         */
        public static RefreshTokenResponseDto withTokenRotation(String accessToken, String newRefreshToken,
                                                                UserDto user, int expiresIn,
                                                                LocalDateTime accessTokenExpiry,
                                                                LocalDateTime refreshTokenExpiry) {
                return RefreshTokenResponseDto.builder()
                        .accessToken(accessToken)
                        .tokenType("Bearer")
                        .refreshToken(newRefreshToken)
                        .expiresIn(expiresIn)
                        .accessTokenExpiry(accessTokenExpiry)
                        .refreshTokenExpiry(refreshTokenExpiry)
                        .user(user)
                        .build();
        }

        /**
         * Crée une réponse sans rotation du refresh token
         */
        public static RefreshTokenResponseDto withoutTokenRotation(String accessToken, String existingRefreshToken,
                                                                   UserDto user, int expiresIn,
                                                                   LocalDateTime accessTokenExpiry) {
                return RefreshTokenResponseDto.builder()
                        .accessToken(accessToken)
                        .tokenType("Bearer")
                        .refreshToken(existingRefreshToken)
                        .expiresIn(expiresIn)
                        .accessTokenExpiry(accessTokenExpiry)
                        .refreshTokenExpiry(null) // Non renouvelé
                        .user(user)
                        .build();
        }

        /**
         * Crée une réponse simple (compatibilité ascendante)
         */
        public static RefreshTokenResponseDto simple(String accessToken, String refreshToken, UserDto user) {
                return RefreshTokenResponseDto.builder()
                        .accessToken(accessToken)
                        .tokenType("Bearer")
                        .refreshToken(refreshToken)
                        .expiresIn(900) // 15 minutes par défaut
                        .accessTokenExpiry(LocalDateTime.now().plusMinutes(15))
                        .user(user)
                        .build();
        }

        // MÉTHODES UTILITAIRES
        /**
         * Vérifie si un nouveau refresh token a été généré
         */
        public boolean isRefreshTokenRotated() {
                return refreshTokenExpiry != null;
        }

        /**
         * Vérifie si le access token est expiré
         */
        public boolean isAccessTokenExpired() {
                if (accessTokenExpiry == null) {
                        return true; // Considérer comme expiré si non spécifié
                }
                return accessTokenExpiry.isBefore(LocalDateTime.now());
        }

        /**
         * Vérifie si le refresh token est expiré
         */
        public boolean isRefreshTokenExpired() {
                if (refreshTokenExpiry == null) {
                        return false; // Pas de refresh token ou pas d'expiration spécifiée
                }
                return refreshTokenExpiry.isBefore(LocalDateTime.now());
        }

        /**
         * Retourne le temps restant avant expiration du access token en secondes
         */
        public long getAccessTokenRemainingTime() {
                if (accessTokenExpiry == null) {
                        return 0;
                }
                return java.time.Duration.between(LocalDateTime.now(), accessTokenExpiry).getSeconds();
        }

        /**
         * Retourne le temps restant avant expiration du refresh token en secondes
         */
        public long getRefreshTokenRemainingTime() {
                if (refreshTokenExpiry == null) {
                        return 0;
                }
                return java.time.Duration.between(LocalDateTime.now(), refreshTokenExpiry).getSeconds();
        }

        /**
         * Vérifie si le access token est sur le point d'expirer (moins de 5 minutes)
         */
        public boolean isAccessTokenAboutToExpire() {
                return getAccessTokenRemainingTime() < 300; // 5 minutes
        }

        /**
         * Vérifie si le refresh token est sur le point d'expirer (moins de 24 heures)
         */
        public boolean isRefreshTokenAboutToExpire() {
                return getRefreshTokenRemainingTime() < 86400; // 24 heures
        }

        /**
         * Retourne un message d'état des tokens
         */
        public String getTokenStatus() {
                if (isAccessTokenExpired()) {
                        return "Access token expiré";
                }
                if (isRefreshTokenExpired()) {
                        return "Refresh token expiré";
                }
                if (isAccessTokenAboutToExpire()) {
                        return "Access token expire bientôt";
                }
                if (isRefreshTokenAboutToExpire()) {
                        return "Refresh token expire bientôt";
                }
                return "Tokens valides";
        }
}