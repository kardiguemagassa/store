package com.store.store.controller;

import com.store.store.constants.ErrorCodes;
import com.store.store.dto.auth.LoginRequestDto;
import com.store.store.dto.auth.LoginResponseDto;
import com.store.store.dto.auth.RegisterRequestDto;
import com.store.store.dto.common.ApiResponse;
import com.store.store.entity.RefreshToken;
import com.store.store.service.IAuthService;
import com.store.store.service.IRefreshTokenService;
import com.store.store.service.ISecurityAlertService;

import com.store.store.service.impl.MessageServiceImpl;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour l'authentification et la gestion des sessions utilisateur.
 *
 * <p><strong>Fonctionnalités principales :</strong>
 * <ul>
 *   <li>🔐 Connexion (login) avec JWT + Refresh Token</li>
 *   <li>📝 Inscription (register) avec validation complète</li>
 *   <li>🔄 Renouvellement de token (refresh) avec rotation</li>
 *   <li>🚪 Déconnexion (logout) avec révocation des tokens</li>
 * </ul>
 *
 * <p><strong>Sécurité avancée :</strong>
 * <ul>
 *   <li>🍪 Refresh token dans cookie HttpOnly</li>
 *   <li>🔍 Détection de replay attacks</li>
 *   <li>🌐 Vérification IP/User-Agent</li>
 *   <li>⏱️ Rate limiting sur /refresh</li>
 *   <li>📧 Alertes de sécurité par email</li>
 * </ul>
 *
 * <p><strong>Architecture de tokens :</strong>
 * <pre>
 * Access Token (JWT)
 * ├─ Stockage : Cookie "accessToken" (HttpOnly)
 * ├─ Durée : 15 minutes
 * ├─ Usage : Authentification des requêtes API
 * └─ Contenu : userId, email, roles
 *
 * Refresh Token (UUID)
 * ├─ Stockage : Cookie "refreshToken" (HttpOnly)
 * ├─ Durée : 7 jours
 * ├─ Usage : Renouvellement du access token
 * └─ Base de données : Stocké en BDD avec metadata
 * </pre>
 *
 * <p><strong>Flux d'authentification :</strong>
 * <pre>
 * 1. POST /login
 *    → Validation credentials
 *    → Génération JWT + Refresh Token
 *    → Stockage tokens dans cookies HttpOnly
 *    → Retour ApiResponse&lt;LoginResponseDto&gt;
 *
 * 2. Access token expire (15 min)
 *    → Frontend appelle POST /refresh
 *    → Validation refresh token (UUID)
 *    → Rotation : ancien token révoqué, nouveau créé
 *    → Retour nouveaux tokens
 *
 * 3. POST /logout
 *    → Révocation du refresh token en BDD
 *    → Suppression des cookies
 *    → Session terminée
 * </pre>
 *
 * @author Kardigué
 * @version 5.0 - Production Ready avec ApiResponse
 * @since 2025-01-01
 *
 * @see IAuthService
 * @see IRefreshTokenService
 * @see ApiResponse
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API d'authentification et de gestion des sessions")
public class AuthController {

    private final IAuthService authService;
    private final IRefreshTokenService refreshTokenService;
    private final ISecurityAlertService securityAlertService;
    private final MessageServiceImpl messageService;

    @Value("${store.refresh-token.expiration-ms:604800000}")
    private long refreshTokenExpirationMs; // 7 jours par défaut

    // ENDPOINT : LOGIN
    @PostMapping("/login")
    @Operation(
            summary = "Connexion utilisateur",
            description = "Authentifie un utilisateur et retourne JWT + Refresh Token dans des cookies HttpOnly"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Connexion réussie",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Identifiants invalides",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("Login attempt for username: {} from IP: {}", loginRequest.username(), ipAddress);

        // Authentification via le service
        LoginResponseDto loginResponseDto = authService.login(loginRequest, ipAddress, userAgent);

        // Ajouter le refresh token dans un cookie HttpOnly
        Cookie refreshTokenCookie = createRefreshTokenCookie(
                loginResponseDto.refreshToken(),
                refreshTokenExpirationMs
        );
        response.addCookie(refreshTokenCookie);

        log.info("Login successful for user: {}", loginRequest.username());

        // Construire ApiResponse avec message localisé
        String successMessage = messageService.getMessage("api.success.auth.login");

        ApiResponse<LoginResponseDto> apiResponse = ApiResponse.success(successMessage, loginResponseDto)
                .withPath("/api/v1/auth/login");

        return ResponseEntity.ok(apiResponse);
    }

    // ENDPOINT : REGISTER
    @PostMapping("/register")
    @Operation(
            summary = "Inscription utilisateur",
            description = "Inscrit un nouvel utilisateur avec validation complète (email unique, password fort, etc.)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Inscription réussie",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données invalides ou email/mobile déjà utilisé",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> registerUser(
            @Valid @RequestBody RegisterRequestDto registerRequestDto) {

        log.debug("Registration attempt for email: {}", registerRequestDto.getEmail());

        authService.registerUser(registerRequestDto);

        log.info("User registered successfully: {}", registerRequestDto.getEmail());

        // Message localisé via MessageService
        String successMessage = messageService.getMessage("api.success.auth.register");

        ApiResponse<Void> response = ApiResponse.<Void>created(successMessage, null)
                .withPath("/api/v1/auth/register");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ENDPOINT : REFRESH TOKEN
    /**
     * + Cookie mis à jour :
     * - refreshToken=660f9511-...; HttpOnly; Secure; SameSite=Lax
     * </pre>
     */
    @PostMapping("/refresh")
    @RateLimiter(name = "refresh", fallbackMethod = "refreshRateLimitFallback")
    @Operation(
            summary = "Renouveler le JWT",
            description = "Renouvelle le JWT en utilisant le refresh token du cookie HttpOnly. " +
                    "Implémente la rotation des tokens et détection des replay attacks."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token renouvelé avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Refresh token invalide, expiré ou absent",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429",
                    description = "Trop de tentatives",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    public ResponseEntity<ApiResponse<LoginResponseDto>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            // 1. Lire le refresh token depuis le cookie
            String refreshTokenValue = getRefreshTokenFromCookie(request);

            if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
                log.warn("No refresh token found in cookie");

                String errorMessage = messageService.getMessage("api.error.auth.refresh.missing");

                ApiResponse<LoginResponseDto> errorResponse = ApiResponse.<LoginResponseDto>unauthorized(
                        ErrorCodes.AUTHENTICATION_REQUIRED,
                        errorMessage,
                        request.getServletPath()
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 2. Extraire IP et User-Agent
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("Refresh token request from IP: {}", ipAddress);

            // 3. Vérifier le refresh token sans le renouveler encore
            RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

            // 4. SÉCURITÉ: Détection de replay attack
            if (refreshToken.isRevoked()) {
                handleReplayAttack(refreshToken, ipAddress, userAgent);
                deleteCookie(response);

                String errorMessage = messageService.getMessage("api.error.auth.replay.attack");

                ApiResponse<LoginResponseDto> errorResponse = ApiResponse.<LoginResponseDto>unauthorized(
                        ErrorCodes.AUTHENTICATION_FAILED,
                        errorMessage,
                        request.getServletPath()
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // 5. SÉCURITÉ: Vérification device/IP matching
            if (!isDeviceMatching(refreshToken, ipAddress, userAgent)) {
                log.warn("Device/IP mismatch for refresh token. Expected IP: {}, Got: {}",
                        refreshToken.getIpAddress(), ipAddress);

                // Notifier l'utilisateur mais continuer - peut être changement de réseau
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

            // 6. Renouveler les tokens (appel au service)
            LoginResponseDto refreshResponse = refreshTokenService.refreshAccessToken(
                    refreshTokenValue, ipAddress, userAgent);

            // 7. Mettre à jour le cookie avec le nouveau refresh token
            Cookie newRefreshTokenCookie = createRefreshTokenCookie(
                    refreshResponse.refreshToken(),
                    refreshTokenExpirationMs
            );
            response.addCookie(newRefreshTokenCookie);

            log.info("Token refreshed successfully");

            // Construire ApiResponse
            String successMessage = messageService.getMessage("api.success.auth.refresh");

            ApiResponse<LoginResponseDto> apiResponse = ApiResponse.success(successMessage, refreshResponse)
                    .withPath("/api/v1/auth/refresh");

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Refresh failed - Invalid token: {}", e.getMessage());
            deleteCookie(response);

            String errorMessage = messageService.getMessage("api.error.auth.refresh.invalid");

            ApiResponse<LoginResponseDto> errorResponse = ApiResponse.<LoginResponseDto>unauthorized(
                    ErrorCodes.AUTHENTICATION_FAILED,
                    errorMessage,
                    request.getServletPath()
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            deleteCookie(response);

            String errorMessage = messageService.getMessage("api.error.internal");

            ApiResponse<LoginResponseDto> errorResponse = ApiResponse.internalError(
                    ErrorCodes.INTERNAL_ERROR,
                    errorMessage,
                    request.getServletPath()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Fallback pour le rate limiting sur /refresh.
     * Déclenché quand le nombre de tentatives dépasse la limite configurée
     * (par défaut 10 requêtes par minute).
     *
     * @param request Requête HTTP
     * @param response Réponse HTTP
     * @param e Exception du rate limiter
     * @return ApiResponse avec code 429 Too Many Requests
     */
    public ResponseEntity<ApiResponse<LoginResponseDto>> refreshRateLimitFallback(
            HttpServletRequest request,
            HttpServletResponse response,
            Exception e) {

        String ipAddress = getClientIpAddress(request);
        log.warn("Rate limit exceeded for refresh endpoint from IP: {}", ipAddress);

        String errorMessage = messageService.getMessage("api.error.rate.limit.exceeded");

        ApiResponse<LoginResponseDto> errorResponse = ApiResponse.<LoginResponseDto>builder()
                .success(false)
                .message(errorMessage)
                .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                .path(request.getServletPath())
                .errorCode(ErrorCodes.BUSINESS_RULE_VIOLATION)
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
    }

    // ENDPOINT : LOGOUT
    @PostMapping("/logout")
    @Operation(
            summary = "Déconnexion",
            description = "Révoque le refresh token et déconnecte l'utilisateur"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Déconnexion réussie",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

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
            // Toujours supprimer le cookie, même en cas d'erreur
            deleteCookie(response);
        }

        log.info("User logged out successfully");

        // Message localisé
        String successMessage = messageService.getMessage("api.success.auth.logout");

        ApiResponse<Void> apiResponse = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/auth/logout");

        return ResponseEntity.ok(apiResponse);
    }

    // MÉTHODES PRIVÉES - SÉCURITÉ

    /**
     * @param refreshToken Token compromis
     * @param ipAddress IP de l'attaquant
     * @param userAgent User-Agent de l'attaquant
     */
    private void handleReplayAttack(RefreshToken refreshToken, String ipAddress, String userAgent) {
        log.error("SECURITY ALERT: Replay attack detected! " +
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
     * @param refreshToken Token original
     * @param currentIp IP actuelle
     * @param currentUserAgent User-Agent actuel
     * @return true si match, false sinon
     */
    private boolean isDeviceMatching(RefreshToken refreshToken, String currentIp, String currentUserAgent) {
        boolean ipMatches = refreshToken.getIpAddress() != null &&
                refreshToken.getIpAddress().equals(currentIp);

        boolean userAgentSimilar = refreshToken.getUserAgent() != null &&
                currentUserAgent != null &&
                areSimilarUserAgents(refreshToken.getUserAgent(), currentUserAgent);

        // Au moins un des deux correspond
        return ipMatches || userAgentSimilar;
    }

    /**
     * @param original User-Agent original
     * @param current User-Agent actuel
     * @return true si même navigateur
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
     * Extrait le nom du navigateur depuis le User-Agent.
     *
     * @param userAgent User-Agent string
     * @return Nom du navigateur (Chrome, Firefox, Safari, etc.)
     */
    private String extractBrowserName(String userAgent) {
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("Opera")) return "Opera";
        return "Unknown";
    }


    // MÉTHODES UTILITAIRES - COOKIES
    /**
     * Récupère le refresh token depuis le cookie de la requête.
     *
     * @param request Requête HTTP
     * @return Valeur du refresh token ou null si absent
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
     * @param refreshToken Valeur du token
     * @param expirationMs Durée de vie en millisecondes
     * @return Cookie configuré
     */
    private Cookie createRefreshTokenCookie(String refreshToken, long expirationMs) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setPath("/");
        cookie.setHttpOnly(true);  // Protection XSS
        cookie.setSecure(true);    // HTTPS uniquement
        cookie.setAttribute("SameSite", "Lax"); // Protection CSRF
        cookie.setMaxAge((int) (expirationMs / 1000)); // Conversion ms → secondes

        return cookie;
    }

    /**
     * Supprime le cookie refresh token.
     * Utilisé lors du logout ou en cas d'erreur d'authentification.
     * @param response Réponse HTTP
     */
    private void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0); // Expire immédiatement
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    // MÉTHODES UTILITAIRES - RÉSEAU
    /**
     * Récupère l'adresse IP réelle du client.
     *
     * @param request Requête HTTP
     * @return Adresse IP du client
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
}