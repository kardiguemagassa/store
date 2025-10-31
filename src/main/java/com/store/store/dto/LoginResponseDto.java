package com.store.store.dto;

import com.store.store.dto.UserDto;

/**
 * DTO pour la réponse de login avec support des refresh tokens.
 *
 * VERSION 2.0: Ajout des champs refreshToken et expiresIn pour
 * supporter le système de refresh automatique côté frontend.
 *
 * @param message Message de confirmation ("Login successful")
 * @param user Données de l'utilisateur connecté

 * @param refreshToken Refresh Token (UUID) - Valide 7 jours, stocké en base
 * @param expiresIn Durée de validité du JWT en secondes (900 = 15 min)
 *
 * @author Kardigué
 * @version 2.0 (Avec Refresh Token)
 * @since 2025-01-27
 */
public record LoginResponseDto(
        String message,
        UserDto user,
        String jwtToken,
        String refreshToken,    // NOUVEAU: UUID refresh token
        Integer expiresIn       // NOUVEAU: 900 (secondes)
) {
}