package com.store.store.util;

import com.store.store.constants.ApplicationConstants;
import com.store.store.entity.Customer;
import com.store.store.security.CustomerUserDetails;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Utilitaire pour la génération et la validation des tokens JWT.
 * Utilise la bibliothèque JJWT pour créer des tokens signés.
 *
 * @author Votre Nom
 * @version 1.0
 * @since 2025-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final Environment env;

    /**
     * Génère un token JWT à partir des informations d'authentification.
     * Le token contient les informations de l'utilisateur (nom, email, mobile, rôles)
     * et est valide pendant 1 heure.
     *
     * @param authentication L'objet Authentication contenant les informations de l'utilisateur
     * @return Le token JWT signé sous forme de String
     * @throws ClassCastException si le principal n'est pas du type CustomerUserDetails
     */
    public String generateJwtToken(Authentication authentication) {
        log.debug("Génération du JWT token pour l'utilisateur: {}", authentication.getName());

        // Récupérer CustomerUserDetails puis extraire Customer
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Customer fetchedCustomer = userDetails.customer();

        // Récupérer la clé secrète depuis la configuration
        String secret = env.getProperty(
                ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE
        );
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Calculer la date d'expiration (1 heure)
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + 60 * 60 * 1000); // 1 heure

        // Construire le JWT avec les claims personnalisés
        String jwt = Jwts.builder()
                .issuer("Eazy Store")
                .subject("JWT Token")
                .claim("username", fetchedCustomer.getName())
                .claim("email", fetchedCustomer.getEmail())
                .claim("mobileNumber", fetchedCustomer.getMobileNumber())
                .claim("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")))
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();

        log.info("JWT token généré avec succès pour l'utilisateur: {} (expire dans 1h)", fetchedCustomer.getEmail());

        return jwt;
    }

    /**
     * Valide un token JWT et extrait les informations qu'il contient.
     *
     * @param token Le token JWT à valider
     * @return true si le token est valide, false sinon
     */
    public boolean validateJwtToken(String token) {
        try {
            String secret = env.getProperty(
                    ApplicationConstants.JWT_SECRET_KEY,
                    ApplicationConstants.JWT_SECRET_DEFAULT_VALUE
            );
            SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            log.debug("JWT token validé avec succès");
            return true;
        } catch (Exception e) {
            log.error("Erreur lors de la validation du JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrait l'email de l'utilisateur depuis le token JWT.
     *
     * @param token Le token JWT
     * @return L'email de l'utilisateur
     */
    public String getEmailFromJwtToken(String token) {
        String secret = env.getProperty(
                ApplicationConstants.JWT_SECRET_KEY,
                ApplicationConstants.JWT_SECRET_DEFAULT_VALUE
        );
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }
}