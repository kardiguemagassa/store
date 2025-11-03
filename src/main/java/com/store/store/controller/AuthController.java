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

 * VERSION 4.0 - SÉCURITÉ AVANCÉE :
 * Refresh token dans cookie HttpOnly
 * Détection de replay attacks
 * Vérification IP/UserAgent
 * Rate limiting
 * Alertes de sécurité
 *
 * @author Kardigué
 * @version 4.0 (Security Enhanced)
 * @since 2025-10-31
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
     * Authenticates a user and returns a JWT along with a Refresh Token.
     *
     * @param loginRequest The login request payload containing user credentials.
     * @param request The HTTP servlet request.
     * @param response The HTTP servlet response to add cookies or modify headers.
     * @return A ResponseEntity containing the LoginResponseDto with the authentication details
     *         including the JWT and Refresh Token.
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
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest,
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
     * Registers a new user with full validation.
     * Validates the input data in the request body and processes the registration.
     *
     * @param registerRequestDto the DTO containing the registration details of the user
     *                           such as email, password, and other required information
     * @return ResponseEntity containing a SuccessResponseDto if the registration is successful
     *         with HTTP status 201 (Created); otherwise, it will throw an appropriate exception
     *         for HTTP status 400 (Bad Request) in case of invalid data or duplicate entries
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
    public ResponseEntity<SuccessResponseDto> registerUser(@Valid @RequestBody RegisterRequestDto registerRequestDto) {

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
     * Refreshes the JWT using the refresh token from the HttpOnly cookie. Implements token rotation
     * and replay attack detection.
     *
     * @param request  the HttpServletRequest object, used to retrieve the refresh token and client details.
     * @param response the HttpServletResponse object, used to update the cookie with a new refresh token.
     * @return a ResponseEntity containing a serialized LoginResponseDto on success or an ErrorResponseDto
     *         on failure. Possible HTTP response codes include:
     *         - 200 (OK): Token successfully refreshed.
     *         - 401 (Unauthorized): Refresh token is invalid, expired, revoked, or missing.
     *         - 429 (Too Many Requests): Rate limiting triggered due to excessive attempts.
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
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {

        try {
            // 1 Lire le refresh token depuis le cookie
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

            // 2 Extraire IP et User-Agent
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            log.info("Refresh token request from IP: {}", ipAddress);

            // 3 Vérifier le refresh token sans le renouveler encore
            RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

            // 4 SÉCURITÉ: Détection de replay attack
            if (refreshToken.isRevoked()) {
                handleReplayAttack(refreshToken, ipAddress, userAgent);
                deleteCookie(response);
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ErrorResponseDto.unauthorized(
                                ErrorCodes.UNAUTHORIZED,
                                "Security alert: Token has been revoked",
                                request.getServletPath()
                        ));
            }

            // 5 SÉCURITÉ: Vérification device/IP matching
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

            // 6 Renouveler les tokens (appel au service)
            LoginResponseDto refreshResponse = refreshTokenService.refreshAccessToken(
                    refreshTokenValue, ipAddress, userAgent);

            // 7 Mettre à jour le cookie avec le nouveau refresh token
            Cookie newRefreshTokenCookie = createRefreshTokenCookie(refreshResponse.refreshToken(),
                    refreshTokenExpirationMs
            );

            response.addCookie(newRefreshTokenCookie);

            log.info("Token refreshed successfully");

            return ResponseEntity.ok(refreshResponse);

        } catch (IllegalArgumentException e) {
            log.warn("Refresh failed - Invalid token: {}", e.getMessage());
            deleteCookie(response);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponseDto.unauthorized(
                            ErrorCodes.UNAUTHORIZED,
                            "Invalid or expired refresh token",
                            request.getServletPath()
                    ));

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            deleteCookie(response);
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
     * Handles requests that exceed the rate limit for the refresh endpoint.
     * Logs the IP address of the client making the request and returns a response
     * indicating that too many attempts have been made.
     *
     * @param request the HttpServletRequest associated with the client's request
     * @param response the HttpServletResponse to provide the HTTP response
     * @param e the exception triggered by exceeding the rate limit
     * @return a ResponseEntity containing the error details and HTTP status code 429 (Too Many Requests)
     */
    public ResponseEntity<?> refreshRateLimitFallback(HttpServletRequest request, HttpServletResponse response,
                                                      Exception e) {

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
     * Logs out the user by revoking the refresh token and removing the associated cookie.
     *
     * @param request  the HTTP servlet request containing the user's session and cookies
     * @param response the HTTP servlet response to modify and delete the cookie
     * @return a ResponseEntity containing a success response DTO indicating successful logout
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
            deleteCookie(response);
        }

        log.info("User logged out successfully");

        return ResponseEntity.ok(
                SuccessResponseDto.success("Déconnexion réussie")
        );
    }


    /**
     * Handles a replay attack by identifying and responding to a potential security breach.
     * It will revoke all tokens associated with the user, log the incident,
     * and notify the user via email about the possible account compromise.
     *
     * @param refreshToken The compromised refresh token that was reused.
     * @param ipAddress The IP address from which the token reuse was detected.
     * @param userAgent The User-Agent string from the client making the request.
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
     * Checks if the device that issued the refresh token matches the current device
     * based on the IP address or similarity of the user agent.
     *
     * @param refreshToken the refresh token containing the device information (IP address and user agent)
     * @param currentIp the current IP address to compare with the IP address from the refresh token
     * @param currentUserAgent the current user agent string to compare with the user agent from the refresh token
     * @return true if either the IP address matches or the user agents are determined to be similar, false otherwise
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
     * Checks if two user agent strings represent similar browsers by comparing their extracted browser names.
     *
     * @param original the user agent string of the original browser; may be null
     * @param current the user agent string of the current browser; may be null
     * @return true if both user agent strings represent browsers with the same name, false otherwise
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
     * Extracts the name of the browser from the provided user agent string.
     *
     * @param userAgent the user agent string from which the browser name is to be extracted
     * @return the name of the browser, such as "Chrome", "Firefox", "Safari", "Edge", "Opera",
     *         or "Unknown" if the browser is not recognized
     */
    private String extractBrowserName(String userAgent) {
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("Opera")) return "Opera";
        return "Unknown";
    }

    /**
     * Retrieves the refresh token from the cookies in the given HTTP request.
     *
     * @param request the HttpServletRequest object containing the cookies
     * @return the value of the refresh token if found; otherwise, null
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
     * Creates a secure HTTP-only cookie containing the specified refresh token and expiration time.
     *
     * @param refreshToken the refresh token value to be stored in the cookie
     * @param expirationMs the expiration time for the cookie in milliseconds
     * @return a configured {@link Cookie} object containing the refresh token
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
     * Deletes the refresh token cookie by setting its value to null,
     * setting its max age to 0, and adding it back to the HTTP response.
     *
     * @param response the HttpServletResponse object used to add the modified cookie
     */
    private void deleteCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    /**
     * Retrieves the client's IP address from the provided HTTP request. It first checks
     * the "X-Forwarded-For" header for any forwarded IP addresses. If present, it extracts
     * the first IP address from the list. If the "X-Forwarded-For" header is not set, it
     * falls back to the "X-Real-IP" header. If neither header is available, it defaults to
     * the remote address associated with the request.
     *
     * @param request the HttpServletRequest object containing client request information
     * @return the client's IP address as a String
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
     * Retrieves a localized message based on the given message code and arguments.
     *
     * @param code the message code for the desired localized message
     * @param args optional arguments to format the message
     * @return the localized message string corresponding to the given code and arguments
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}   