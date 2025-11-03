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
 * Centralized utility for generating and validating JWT tokens.

 * RESPONSIBILITIES:
 * - JWT generation from Authentication (login)
 * - JWT generation from Customer (refresh token)
 * - JWT validation (signature + expiration)
 * - Claim extraction (username/email, roles)

 * SECURITY:
 * - Secret key from application.yml (NEVER hardcoded)
 * - HMAC-SHA512 signature
 * - Configurable expiration (default: 15 minutes)
 * - Configurable issuer for multi-tenancy

 * ARCHITECTURE:
 * - Centralized: a single source of truth for JWT
 * - Reusable: login, refresh, filter
 * - Testable: all methods are stateless

 * @author Kardigué
 * @version 3.0 - Production Ready
 * @since 2025-10-27
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
     * Generates a signed JWT (JSON Web Token) for the authenticated user.
     * This token includes user details like email, name, mobile number,
     * and roles, and is used for ensuring secure communication and
     * authorization.
     *
     * @param authentication The authentication object containing user credentials
     *                        and authorities. Must not be null.
     * @return A signed JWT as a String.
     * @throws IllegalArgumentException If the authentication object is null.
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

        log.info("JWT generated for user: {} (expires in {}ms)", customer.getEmail(), jwtExpirationMs);

        return jwt;
    }

    /**
     * Generates a signed JWT (JSON Web Token) for the provided customer.
     * The token includes customer details such as email, name, mobile number,
     * and roles, which are essential for secure communication and authorization.
     *
     * @param customer The customer object containing user details and roles.
     *                 Must not be null.
     * @return A signed JWT as a string.
     * @throws IllegalArgumentException If the customer object is null.
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
        String jwt = buildJwtToken(customer.getEmail(), customer.getName(), customer.getMobileNumber(), roles);

        log.info("JWT generated from customer: {} (expires in {}ms)",
                customer.getEmail(), jwtExpirationMs);

        return jwt;
    }

    /**
     * Builds a signed JWT (JSON Web Token) with provided user details and roles.
     * The token is signed using HMAC-SHA512 and includes standard claims as well
     * as additional custom claims (email, name, mobile, roles).
     *
     * @param email The email of the user to be included in the token. This will also serve as the subject.
     * @param name The full name of the user to be included as a custom claim.
     * @param mobile The mobile number of the user to be included as a custom claim.
     * @param roles The roles assigned to the user, provided as a comma-separated string, to be included as a custom claim.
     * @return A signed JWT as a String.
     */
    private String buildJwtToken(String email, String name, String mobile, String roles) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtExpirationMs);

        SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()

                // CLAIMS STANDARDS (Registered Claims - RFC 7519)
                .issuer(jwtIssuer)              // iss: Émetteur du token
                .subject(email)                 // sub: Identifiant unique (email)
                .issuedAt(now)                  // iat: Date de création
                .expiration(expirationDate)     // exp: Date d'expiration

                // CLAIMS CUSTOM (Private Claims)
                .claim("email", email)          // Email (pour compatibilité)
                .claim("name", name)            // Nom complet
                .claim("mobile", mobile)        // Numéro de téléphone
                .claim("roles", roles)          // Rôles (comma-separated)


                // SIGNATURE (HMAC-SHA512)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates a given JSON Web Token (JWT) to ensure it is well-formed, unexpired,
     * correctly signed, and meets expected standards.
     *
     * @param token The JWT string to validate. It must not be null or blank.
     * @return true if the token is valid; false if the token is expired, malformed,
     *         or fails signature validation.
     * @throws IllegalArgumentException if the provided token is null or blank.
     */
    public boolean validateJwtToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            // Parser et valider le token (lance une exception si invalide)
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);

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
     * Extracts the username (email) from a given JWT (JSON Web Token).
     * The method parses the JWT, verifies its validity using the configured
     * secret key, and retrieves the email embedded as the subject in the token's claims.
     *
     * @param token The JWT string to be parsed. It must not be null or blank.
     *              If the token is invalid or does not contain a subject, an
     *              IllegalArgumentException will be thrown.
     * @return The extracted username (email) as a string.
     * @throws IllegalArgumentException If the token is null, blank, invalid,
     *                                  or does not contain a subject.
     */
    public String getUsernameFromJwtToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

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
     * Extracts the email address from the given JWT (JSON Web Token).
     * This method internally delegates the operation to `getUsernameFromJwtToken`,
     * as the email is expected to be stored as the username (subject) in the token's claims.
     *
     * @param token The JWT string to be parsed. It must not be null or blank.
     *              If the token is invalid or does not contain a subject, an
     *              IllegalArgumentException will be thrown.
     * @return The extracted email address as a string.
     * @throws IllegalArgumentException If the token is null, blank, invalid,
     *                                  or does not contain a subject.
     */
    public String getEmailFromJwtToken(String token) {
        return getUsernameFromJwtToken(token);
    }

    /**
     * Extracts the roles information from a given JWT (JSON Web Token).
     * This method parses the JWT, verifies its validity using the configured secret key,
     * and retrieves the roles embedded as a claim in the token.
     *
     * @param token The JWT string to be parsed. It must not be null or blank.
     *              If the token is invalid, missing roles, or fails parsing,
     *              an empty string is returned.
     * @return The extracted roles as a comma-separated string.
     *         Returns an empty string if roles are missing or if any error occurs during parsing.
     * @throws IllegalArgumentException If the provided token is null or blank.
     */
    public String getRolesFromJwtToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

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
     * Retrieves all claims from the provided JSON Web Token (JWT).
     * This method parses the JWT, validates its signature using
     * the configured secret key, and returns the claims contained within the token.
     *
     * @param token The JWT string to be parsed. It must not be null or blank.
     *              If the token is null, blank, or invalid, an IllegalArgumentException
     *              will be thrown.
     * @return The claims extracted from the token as a Claims object.
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