package com.store.store.dto;

/**
 * Data Transfer Object representing the response of a login operation.
 * This includes a message, user details, tokens, and expiration information.
 *
 * @param message       A message indicating the status of the login operation.
 * @param user          An instance of {@code UserDto} representing details of the logged-in user.
 * @param jwtToken      The generated JWT token for authentication purposes.
 * @param refreshToken  A UUID refresh token used for obtaining a new JWT token.
 * @param expiresIn     The expiration time (in seconds) of the JWT token, typically 900 seconds.
 */
public record LoginResponseDto(
        String message,
        UserDto user,
        String jwtToken,
        String refreshToken,    // NOUVEAU: UUID refresh token
        Integer expiresIn       // NOUVEAU: 900 (secondes)
) {
}