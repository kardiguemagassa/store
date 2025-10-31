package com.store.store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Point d'entrée centralisé pour les erreurs d'authentification (401).
 *
 * RESPONSABILITÉ:
 * Gérer toutes les erreurs 401 Unauthorized de manière cohérente
 * en retournant une réponse JSON standardisée.
 *
 * APPELÉ QUAND:
 * - JWT absent et endpoint protégé accédé
 * - JWT invalide
 * - JWT expiré
 * - User non authentifié essaie d'accéder à un endpoint protégé
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-01-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();

        log.warn("Unauthorized access attempt: {} {} - Reason: {}",
                method, requestURI, authException.getMessage());

        // Construire la réponse d'erreur standardisée
        ErrorResponseDto errorResponse = ErrorResponseDto.unauthorized(
                "AUTHENTICATION_REQUIRED",                          // errorCode
                "Authentication required to access this resource",  // message
                requestURI                                          // path
        );

        // Configurer la réponse HTTP
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Écrire la réponse JSON
        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }
}