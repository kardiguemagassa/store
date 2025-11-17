package com.store.store.dto.auth;

import com.store.store.dto.user.UserDto;

/**
 * Objet de transfert de données représentant la réponse à une opération de connexion.
 * Il contient un message, les informations de l'utilisateur, les jetons et les informations d'expiration.
 * @param message Un message indiquant l'état de l'opération de connexion.
 * @param user Une instance de {@code UserDto} représentant les informations de l'utilisateur connecté.
 * @param jwtToken Le jeton JWT généré à des fins d'authentification.
 * @param refreshToken Un jeton d'actualisation UUID utilisé pour obtenir un nouveau jeton JWT.
 * @param expiresIn La durée de validité (en secondes) du jeton JWT, généralement 900 secondes.
 */
public record LoginResponseDto(
        String message,
        UserDto user,
        String jwtToken,
        String refreshToken,    // NOUVEAU: UUID refresh token
        Integer expiresIn       // NOUVEAU: 900 (secondes)
) {
}