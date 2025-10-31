package com.store.store.service;

import com.store.store.dto.LoginResponseDto;
import com.store.store.entity.Customer;
import com.store.store.entity.RefreshToken;

import java.util.List;

/**
 * Interface du service de gestion des Refresh Tokens.
 *
 * @author Kardigué
 * @version 3.0 (Ajout de refreshAccessToken pour cookie HttpOnly)
 * @since 2025-01-01
 */
public interface IRefreshTokenService {

    /**
     * Crée un nouveau refresh token pour un customer.
     *
     * @param customer Le customer
     * @param ipAddress L'adresse IP du client (peut être null)
     * @param userAgent Le User-Agent du client (peut être null)
     * @return Le refresh token créé
     */
    RefreshToken createRefreshToken(Customer customer, String ipAddress, String userAgent);

    /**
     * Vérifie qu'un refresh token est valide (existe, non expiré, non révoqué).
     *
     * @param token Le token à vérifier
     * @return Le RefreshToken si valide
     * @throws IllegalArgumentException si le token est invalide
     */
    RefreshToken verifyRefreshToken(String token);

    /**
     * ✅ NOUVEAU: Renouvelle le JWT en utilisant un refresh token valide.
     *
     * Cette méthode :
     * 1. Vérifie la validité du refresh token
     * 2. Vérifie l'IP et le User-Agent (sécurité)
     * 3. Révoque l'ancien refresh token
     * 4. Génère un nouveau JWT
     * 5. Génère un nouveau refresh token (rotation)
     * 6. Retourne LoginResponseDto avec le nouveau JWT et refresh token
     *
     * @param token Le refresh token à utiliser
     * @param ipAddress L'adresse IP du client (pour vérification de sécurité)
     * @param userAgent Le User-Agent du client (pour vérification de sécurité)
     * @return LoginResponseDto contenant le nouveau JWT, refresh token et infos user
     * @throws IllegalArgumentException si le token est invalide ou si l'IP/UserAgent ne correspond pas
     */
    LoginResponseDto refreshAccessToken(String token, String ipAddress, String userAgent);

    /**
     * Révoque un refresh token spécifique.
     *
     * @param token Le token à révoquer
     */
    void revokeRefreshToken(String token);

    /**
     * Révoque tous les refresh tokens d'un customer.
     * Utilisé en cas de compromission de compte.
     *
     * @param customerId L'ID du customer
     */
    void revokeAllTokensForCustomer(Long customerId);

    /**
     * Supprime les refresh tokens expirés.
     * Appelé par le scheduler de nettoyage.
     *
     * @return Le nombre de tokens supprimés
     */
    int deleteExpiredTokens();

    /**
     * Récupère tous les refresh tokens actifs d'un customer.
     *
     * @param customerId L'ID du customer
     * @return Liste des refresh tokens actifs
     */
    List<RefreshToken> getActiveTokensForCustomer(Long customerId);
}