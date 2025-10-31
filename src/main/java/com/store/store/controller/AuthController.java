package com.store.store.controller;

import com.store.store.constants.ErrorCodes;
import com.store.store.dto.*;
import com.store.store.entity.RefreshToken;
import com.store.store.service.IAuthService;
import com.store.store.service.IRefreshTokenService;
import com.store.store.service.ISecurityAlertService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'authentification et l'inscription des utilisateurs.
 *
 * VERSION 4.0 - SÉCURITÉ AVANCÉE :
 * ✅ Refresh token dans cookie HttpOnly
 * ✅ Détection de replay attacks
 * ✅ Vérification IP/UserAgent
 * ✅ Rate limiting
 * ✅ Alertes de sécurité
 *
 * @author Kardigué
 * @version 4.0 (Security Enhanced)
 * @since 2025-01-31
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API d'authentification et d'inscription")
public class AuthController {

    private final IAuthService authService;
    private final IRefreshTokenService refreshTokenService;
    private final ISecurityAlertService securityAlertService;
    private final MessageSource messageSource;

    @Value("${store.refresh-token.expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Authentifie un utilisateur et retourne un JWT + Refresh Token.
     */
    @PostMapping("/login")
    @Operation(
            summary = "Connexion utilisateur",
            description = "Authentifie un utilisateur et retourne JWT + Refresh Token"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Connexion réussie",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Identifiants invalides",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        LoginResponseDto loginResponse = authService.login(loginRequest, ipAddress, userAgent);

        Cookie refreshTokenCookie = createRefreshTokenCookie(
                loginResponse.refreshToken(),
                refreshTokenExpirationMs
        );
        response.addCookie(refreshTokenCookie);

        log.info("Login successful for user: {}", loginRequest.username());

        return ResponseEntity.ok(loginResponse);
    }

    /**
     * Inscrit un nouvel utilisateur.
     */
    @PostMapping("/register")
    @Operation(
            summary = "Inscription utilisateur",
            description = "Inscrit un nouvel utilisateur avec validation complète"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Inscription réussie",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données invalides ou doublons détectés",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    public ResponseEntity<SuccessResponseDto> registerUser(
            @Valid @RequestBody RegisterRequestDto registerRequestDto
    ) {
        log.debug("Registration attempt for email: {}", registerRequestDto.getEmail());

        authService.registerUser(registerRequestDto);

        log.info("User registered successfully: {}", registerRequestDto.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponseDto.created(
                        getLocalizedMessage("success.user.registered")
                ));
    }

    /**
     * ✅ REFRESH TOKEN - Renouvellement du JWT avec sécurité avancée
     *
     * SÉCURITÉ:
     * - Lit le refresh token depuis le cookie HttpOnly
     * - Détecte les replay attacks (token révoqué réutilisé)
     * - Vérifie l'IP et le User-Agent
     * - Token rotation automatique
     * - Rate limiting
     * - Alertes email en cas d'anomalie
     */
    @PostMapping("/refresh")
    @RateLimiter(name = "refresh", fallbackMethod = "refreshRateLimitFallback")
    @Operation(
            summary = "Renouveler le JWT",
            description = "Renouvelle le JWT en utilisant le refresh token du cookie HttpOnly. " +
                    "Implémente la rotation des tokens et détection des replay attacks."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token renouvelé avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token invalide, expiré ou absent",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Trop de tentatives",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)
                    )
            )
    })
    public ResponseEntity<?> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            // 1️⃣ Lire le refresh token depuis le cookie
            String refreshTokenValue = getRefreshTokenFromCookie(request);

            if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
                log.warn("No refresh token found in cookie");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponseDto.unauthorized(
                                ErrorCodes.UNAUTHORIZED,
                                "No refresh token provided",
                                request.getServletPath()
                        ));
            }

            // 2️⃣ Extraire IP et User-Agent
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("Refresh token request from IP: {}", ipAddress);

            // 3️⃣ Vérifier le refresh token (sans le renouveler encore)
            RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

            // 4️⃣ 🚨 SÉCURITÉ: Détection de replay attack
            if (refreshToken.isRevoked()) {
                handleReplayAttack(refreshToken, ipAddress, userAgent);
                deleteCookie(response, "refreshToken");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponseDto.unauthorized(
                                ErrorCodes.UNAUTHORIZED,
                                "Security alert: Token has been revoked",
                                request.getServletPath()
                        ));
            }

            // 5️⃣ 🛡️ SÉCURITÉ: Vérification device/IP matching
            if (!isDeviceMatching(refreshToken, ipAddress, userAgent)) {
                log.warn("Device/IP mismatch for refresh token. Expected IP: {}, Got: {}",
                        refreshToken.getIpAddress(), ipAddress);

                // Notifier l'utilisateur (mais continuer - peut être changement de réseau)
                try {
                    securityAlertService.notifyNewDeviceLogin(
                            refreshToken.getCustomer(),
                            ipAddress,
                            userAgent
                    );
                } catch (Exception e) {
                    log.error("Failed to send new device alert: {}", e.getMessage());
                }
            }

            // 6️⃣ Renouveler les tokens (appel au service)
            LoginResponseDto refreshResponse = refreshTokenService.refreshAccessToken(
                    refreshTokenValue,
                    ipAddress,
                    userAgent
            );

            // 7️⃣ Mettre à jour le cookie avec le nouveau refresh token
            Cookie newRefreshTokenCookie = createRefreshTokenCookie(
                    refreshResponse.refreshToken(),
                    refreshTokenExpirationMs
            );
            response.addCookie(newRefreshTokenCookie);

            log.info("Token refreshed successfully");

            return ResponseEntity.ok(refreshResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Refresh failed - Invalid token: {}", e.getMessage());
            deleteCookie(response, "refreshToken");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDto.unauthorized(
                            ErrorCodes.UNAUTHORIZED,
                            "Invalid or expired refresh token",
                            request.getServletPath()
                    ));

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            deleteCookie(response, "refreshToken");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDto.unauthorized(
                            ErrorCodes.UNAUTHORIZED,
                            "Token refresh failed: " + e.getMessage(),
                            request.getServletPath()
                    ));
        }
    }

    /**
     * ✅ Fallback pour rate limiting sur /refresh
     */
    public ResponseEntity<?> refreshRateLimitFallback(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception e
    ) {
        String ipAddress = getClientIpAddress(request);
        log.warn("Rate limit exceeded for refresh endpoint from IP: {}", ipAddress);

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponseDto.of(
                        HttpStatus.TOO_MANY_REQUESTS,
                        ErrorCodes.UNAUTHORIZED,
                        "Too many refresh attempts. Please try again later.",
                        request.getServletPath()
                ));
    }

    /**
     * ✅ LOGOUT - Déconnexion
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Déconnexion",
            description = "Révoque le refresh token et déconnecte l'utilisateur"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Déconnexion réussie",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SuccessResponseDto.class)
                    )
            )
    })
    public ResponseEntity<SuccessResponseDto> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            log.debug("Logout attempt");

            String refreshToken = getRefreshTokenFromCookie(request);

            if (refreshToken != null && !refreshToken.isEmpty()) {
                refreshTokenService.revokeRefreshToken(refreshToken);
                log.info("Refresh token revoked successfully");
            } else {
                log.warn("No refresh token found in cookie during logout");
            }

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
        } finally {
            deleteCookie(response, "refreshToken");
        }

        log.info("User logged out successfully");

        return ResponseEntity.ok(
                SuccessResponseDto.success("Déconnexion réussie")
        );
    }

    // ============================================
    // MÉTHODES DE SÉCURITÉ PRIVÉES
    // ============================================

    /**
     * 🚨 Gère les tentatives de replay attack
     *
     * Quand un token révoqué est réutilisé, cela indique :
     * - Vol de token possible
     * - Compte potentiellement compromis
     *
     * Actions :
     * 1. Révoquer TOUS les tokens de l'utilisateur
     * 2. Envoyer email d'alerte
     * 3. Logger l'incident
     */
    private void handleReplayAttack(RefreshToken refreshToken, String ipAddress, String userAgent) {
        log.error("🚨 SECURITY ALERT: Replay attack detected! " +
                        "Revoked refresh token reused for user: {} from IP: {} with User-Agent: {}",
                refreshToken.getCustomer().getEmail(), ipAddress, userAgent);

        try {
            // Révoquer TOUS les tokens de l'utilisateur
            refreshTokenService.revokeAllTokensForCustomer(
                    refreshToken.getCustomer().getCustomerId()
            );

            log.info("All tokens revoked for user: {} due to security breach",
                    refreshToken.getCustomer().getEmail());

            // Envoyer email d'alerte
            securityAlertService.notifyPossibleAccountCompromise(
                    refreshToken.getCustomer(),
                    ipAddress,
                    userAgent,
                    "Replay Attack - Token révoqué réutilisé"
            );

            log.info("Security alert email sent to user: {}",
                    refreshToken.getCustomer().getEmail());

        } catch (Exception e) {
            log.error("Failed to complete security actions during replay attack handling: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * 🛡️ Vérifie si le device/IP correspond au token original
     *
     * Stratégie FLEXIBLE :
     * - Compare IP et User-Agent
     * - Permet changement de réseau (WiFi → 4G)
     * - Alerte si différence détectée
     */
    private boolean isDeviceMatching(RefreshToken refreshToken, String currentIp, String currentUserAgent) {
        boolean ipMatches = refreshToken.getIpAddress() != null &&
                refreshToken.getIpAddress().equals(currentIp);

        boolean userAgentSimilar = refreshToken.getUserAgent() != null &&
                currentUserAgent != null &&
                areSimilarUserAgents(refreshToken.getUserAgent(), currentUserAgent);

        // Stratégie FLEXIBLE : Au moins un des deux correspond
        return ipMatches || userAgentSimilar;
    }

    /**
     * Compare deux User-Agent pour vérifier s'ils sont similaires
     */
    private boolean areSimilarUserAgents(String original, String current) {
        if (original == null || current == null) {
            return false;
        }

        String originalBrowser = extractBrowserName(original);
        String currentBrowser = extractBrowserName(current);

        return originalBrowser.equals(currentBrowser);
    }

    /**
     * Extrait le nom du navigateur depuis le User-Agent
     */
    private String extractBrowserName(String userAgent) {
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("Opera")) return "Opera";
        return "Unknown";
    }

    // ============================================
    // MÉTHODES UTILITAIRES
    // ============================================

    /**
     * Extraire le refresh token depuis les cookies
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Créer un Cookie HttpOnly sécurisé pour le Refresh Token
     */
    private Cookie createRefreshTokenCookie(String refreshToken, long expirationMs) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge((int) (expirationMs / 1000));

        return cookie;
    }

    /**
     * Supprimer un cookie
     */
    private void deleteCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * Récupérer l'adresse IP réelle du client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Récupère un message localisé
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}