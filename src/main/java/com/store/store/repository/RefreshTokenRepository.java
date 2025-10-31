package com.store.store.repository;

import com.store.store.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des Refresh Tokens.
 *
 * @author Kardigué
 * @version 2.1
 * @since 2025-01-27
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Recherche un refresh token par sa valeur.
     *
     * @param token Le token à rechercher
     * @return Optional contenant le RefreshToken si trouvé
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Récupère tous les tokens actifs (non révoqués) d'un customer.
     *
     * @param customerId L'ID du customer
     * @return Liste des tokens actifs
     */
    List<RefreshToken> findByCustomer_CustomerIdAndRevokedFalse(Long customerId);

    /**
     * Récupère tous les tokens d'un customer (révoqués ou non).
     *
     * @param customerId L'ID du customer
     * @return Liste de tous les tokens
     */
    List<RefreshToken> findByCustomer_CustomerId(Long customerId);

    /**
     * Supprime tous les tokens expirés.
     * CORRECTION: Utiliser Instant au lieu de LocalDateTime
     *
     * @param now La date actuelle
     * @return Le nombre de tokens supprimés
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    int deleteByExpiryDateBefore(@Param("now") Instant now);

    /**
     * Compte le nombre de tokens actifs pour un customer.
     *
     * @param customerId L'ID du customer
     * @return Le nombre de tokens actifs
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.customer.customerId = :customerId AND rt.revoked = false AND rt.expiryDate > :now")
    long countActiveTokensByCustomer(@Param("customerId") Long customerId, @Param("now") Instant now);

    /**
     * Vérifie si un token existe et est valide.
     *
     * @param token Le token à vérifier
     * @param now La date actuelle
     * @return true si le token existe, n'est pas révoqué et n'est pas expiré
     */
    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM RefreshToken rt WHERE rt.token = :token AND rt.revoked = false AND rt.expiryDate > :now")
    boolean isTokenValid(@Param("token") String token, @Param("now") Instant now);

    /**
     * Révoque tous les tokens d'un customer.
     * Utilisé en cas de compromission de compte.
     *
     * @param customerId L'ID du customer
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.customer.customerId = :customerId AND rt.revoked = false")
    int revokeAllTokensByCustomer(@Param("customerId") Long customerId);
}