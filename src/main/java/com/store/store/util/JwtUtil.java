// src/main/java/com/store/store/util/JwtUtil.java

package com.store.store.util;

import com.store.store.entity.Customer;
import com.store.store.security.CustomerUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Utilitaire centralisé pour la génération et validation des tokens JWT.
 *
 * RESPONSABILITÉS:
 * - Génération de JWT depuis Authentication (login)
 * - Génération de JWT depuis Customer (refresh token)
 * - Validation de JWT (signature + expiration)
 * - Extraction des claims (username/email, roles)
 *
 * SÉCURITÉ:
 * - Secret key depuis application.yml (JAMAIS hardcodé)
 * - Signature HMAC-SHA512
 * - Expiration configurable (défaut: 15 minutes)
 * - Issuer configurable pour multi-tenancy
 *
 * ARCHITECTURE:
 * - Centralisé: une seule source de vérité pour JWT
 * - Réutilisable: login, refresh, filter
 * - Testable: toutes les méthodes sont stateless
 *
 * @author Kardigué
 * @version 3.0 - Production Ready
 * @since 2025-01-27
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${store.jwt.secret}")
    private String jwtSecret;

    @Value("${store.jwt.expiration-ms:900000}")
    private long jwtExpirationMs;

    @Value("${store.jwt.issuer:store-api}")
    private String jwtIssuer;

    /**
     * ✅ Génère un JWT depuis un objet Authentication (utilisé au LOGIN).
     *
     * STRUCTURE DU JWT:
     * {
     *   "iss": "store-api",
     *   "sub": "user@example.com",
     *   "email": "user@example.com",
     *   "name": "John Doe",
     *   "mobile": "+33612345678",
     *   "roles": "ROLE_USER,ROLE_ADMIN",
     *   "iat": 1706371200,
     *   "exp": 1706372100
     * }
     *
     * @param authentication Objet Authentication de Spring Security
     * @return JWT signé
     * @throws IllegalArgumentException Si authentication est null
     */
    public String generateJwtToken(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }

        // Extraire CustomerUserDetails
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Customer customer = userDetails.customer();

        // Générer le token
        String jwt = buildJwtToken(
                customer.getEmail(),     // subject
                customer.getName(),      // name
                customer.getMobileNumber(), // mobile
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","))  // roles (comma-separated)
        );

        log.info("JWT generated for user: {} (expires in {}ms)",
                customer.getEmail(), jwtExpirationMs);

        return jwt;
    }

    /**
     * ✅ Génère un JWT depuis un Customer (utilisé au REFRESH TOKEN).
     *
     * Cette méthode est appelée par RefreshTokenController quand
     * l'utilisateur utilise son refresh token pour obtenir un nouveau JWT.
     *
     * @param customer Customer entity
     * @return JWT signé
     * @throws IllegalArgumentException Si customer est null
     */
    public String generateJwtTokenFromCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }

        // Extraire les rôles du customer
        String roles = customer.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.joining(","));

        // Générer le token
        String jwt = buildJwtToken(
                customer.getEmail(),
                customer.getName(),
                customer.getMobileNumber(),
                roles
        );

        log.info("JWT generated from customer: {} (expires in {}ms)",
                customer.getEmail(), jwtExpirationMs);

        return jwt;
    }

    /**
     * ✅ Construction interne du JWT (DRY - Don't Repeat Yourself).
     *
     * Cette méthode privée factorise la logique de construction
     * pour éviter la duplication entre generateJwtToken() et
     * generateJwtTokenFromCustomer().
     *
     * @param email Email de l'utilisateur (utilisé comme subject)
     * @param name Nom complet
     * @param mobile Numéro de téléphone
     * @param roles Rôles séparés par virgule (ex: "ROLE_USER,ROLE_ADMIN")
     * @return JWT signé
     */
    private String buildJwtToken(String email, String name, String mobile, String roles) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpirationMs);

        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                // ═══════════════════════════════════════════════════════
                // CLAIMS STANDARDS (Registered Claims - RFC 7519)
                // ═══════════════════════════════════════════════════════
                .issuer(jwtIssuer)              // iss: Émetteur du token
                .subject(email)                 // sub: Identifiant unique (email)
                .issuedAt(now)                  // iat: Date de création
                .expiration(expirationDate)     // exp: Date d'expiration

                // ═══════════════════════════════════════════════════════
                // CLAIMS CUSTOM (Private Claims)
                // ═══════════════════════════════════════════════════════
                .claim("email", email)          // Email (pour compatibilité)
                .claim("name", name)            // Nom complet
                .claim("mobile", mobile)        // Numéro de téléphone
                .claim("roles", roles)          // Rôles (comma-separated)

                // ═══════════════════════════════════════════════════════
                // SIGNATURE (HMAC-SHA512)
                // ═══════════════════════════════════════════════════════
                .signWith(secretKey)

                .compact();
    }

    /**
     * ✅ Valide l'authenticité et l'expiration d'un token JWT.
     *
     * VÉRIFICATIONS:
     * 1. Signature valide (HMAC-SHA512)
     * 2. Token non expiré
     * 3. Format correct
     *
     * EXCEPTIONS GÉRÉES:
     * - ExpiredJwtException: Token expiré
     * - SignatureException: Signature invalide (token modifié)
     * - MalformedJwtException: Format JWT invalide
     *
     * @param token Token JWT à valider
     * @return true si valide, false sinon
     * @throws IllegalArgumentException Si token est null ou vide
     */
    public boolean validateJwtToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            // Parser et valider le token (lance une exception si invalide)
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            log.debug("JWT token validated successfully");
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return false;

        } catch (SignatureException e) {
            log.error("JWT signature validation failed: {}", e.getMessage());
            return false;

        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
            return false;

        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * ✅ Extrait le username (email) depuis un token JWT.
     *
     * Cette méthode est utilisée par JwtAuthenticationFilter pour
     * récupérer l'email de l'utilisateur et charger ses détails depuis la DB.
     *
     * CLAIM UTILISÉ: "sub" (subject) qui contient l'email
     *
     * @param token Token JWT valide
     * @return Email de l'utilisateur
     * @throws IllegalArgumentException Si token invalide ou email absent
     */
    public String getUsernameFromJwtToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extraire le subject (qui contient l'email)
            String email = claims.getSubject();

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Subject (email) is missing in JWT token");
            }

            log.debug("Username extracted from JWT: {}", email);

            return email;

        } catch (Exception e) {
            log.error("Error extracting username from JWT: {}", e.getMessage());
            throw new IllegalArgumentException("Cannot extract username from JWT token", e);
        }
    }

    /**
     * ✅ Extrait l'email depuis un token JWT.
     *
     * NOTE: Cette méthode est un alias de getUsernameFromJwtToken()
     * pour la rétrocompatibilité.
     *
     * @param token Token JWT valide
     * @return Email de l'utilisateur
     */
    public String getEmailFromJwtToken(String token) {
        return getUsernameFromJwtToken(token);
    }

    /**
     * ✅ Extrait les rôles depuis un token JWT.
     *
     * UTILISATION:
     * Peut être utilisé pour afficher les rôles dans les logs
     * ou pour des vérifications additionnelles.
     *
     * @param token Token JWT valide
     * @return Rôles séparés par virgule (ex: "ROLE_USER,ROLE_ADMIN")
     */
    public String getRolesFromJwtToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String roles = claims.get("roles", String.class);

            if (roles == null || roles.isBlank()) {
                log.warn("Roles claim is missing in JWT token");
                return "";
            }

            return roles;

        } catch (Exception e) {
            log.error("Error extracting roles from JWT: {}", e.getMessage());
            return "";
        }
    }

    /**
     * ✅ Extrait tous les claims depuis un token JWT.
     *
     * UTILISATION:
     * Pour debugging ou logging avancé.
     *
     * @param token Token JWT valide
     * @return Claims du JWT
     */
    public Claims getAllClaimsFromJwtToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}