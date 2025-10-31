package com.store.store.service;

import com.store.store.dto.LoginRequestDto;
import com.store.store.dto.LoginResponseDto;
import com.store.store.dto.RegisterRequestDto;

/**
 * Interface du service d'authentification avec support des refresh tokens.
 *
 * Définit les opérations d'authentification disponibles :
 * - Inscription de nouveaux utilisateurs
 * - Connexion avec génération de JWT + Refresh Token
 *
 * @author Kardigué
 * @version 3.0 (JWT + Cookies)
 * @since 2025-01-27
 */
public interface IAuthService {

    /**
     * Inscrit un nouvel utilisateur.
     *
     * Effectue les validations suivantes :
     * - Le mot de passe n'est pas compromis
     * - L'email n'existe pas déjà
     * - Le numéro de téléphone n'existe pas déjà
     *
     * @param request Données d'inscription validées
     * @throws com.store.store.exception.ValidationException si validation échoue
     */
    void registerUser(RegisterRequestDto request);

    /**
     * Authentifie un utilisateur et génère un JWT + Refresh Token.
     *
     * VERSION 3.0: Cette méthode génère maintenant DEUX tokens:
     * - Access Token (JWT) - 15 minutes
     * - Refresh Token (UUID) - 7 jours, stocké en base de données avec IP et UserAgent
     *
     * L'extraction de l'IP et du User-Agent est maintenant faite dans le Controller
     * pour simplifier les tests et la logique métier.
     *
     * @param request Identifiants de connexion (username, password)
     * @param ipAddress Adresse IP du client (pour audit)
     * @param userAgent User-Agent du navigateur (pour sécurité)
     * @return LoginResponseDto avec message, user, jwtToken, refreshToken et expiresIn
     * @throws org.springframework.security.core.AuthenticationException si authentification échoue
     */
    LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent);
}