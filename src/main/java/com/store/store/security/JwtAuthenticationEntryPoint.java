package com.store.store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.constants.ErrorCodes;
import com.store.store.dto.common.ApiResponse;
import com.store.store.service.impl.MessageServiceImpl;
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

/**
 * Point d'entrée pour la gestion des tentatives d'accès non authentifiées.
 * Cette classe implémente {@link AuthenticationEntryPoint} de Spring Security
 * et est déclenchée automatiquement lorsqu'un utilisateur non authentifié tente
 * d'accéder à une ressource protégée nécessitant une authentification.

 * @author Kardigué
 * @version 4.0
 * @since 2025-01-01
 *
 * @see AuthenticationEntryPoint
 * @see ApiResponse
 * @see MessageServiceImpl
 * @see ErrorCodes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * ObjectMapper pour la sérialisation JSON thread-safe.
     *Utilisé pour convertir l'objet ApiResponse en JSON lors de
     * l'écriture de la réponse HTTP.
     */
    private final ObjectMapper objectMapper;

    /**
     * Service de gestion des messages internationalisés.
     * Utilisé pour récupérer les messages d'erreur localisés depuis
     * messages.properties en fonction de la locale de l'utilisateur.
     * Exemple de clé utilisée :
     * api.error.auth.required=Authentification requise pour accéder à cette ressource
     */
    private final MessageServiceImpl messageService;

    /**
     * Gère les tentatives d'accès non authentifiées à des ressources protégées.
     *
     * Cette méthode est automatiquement invoquée par Spring Security lorsque :
     * Un utilisateur non authentifié tente d'accéder à un endpoint protégé</li>
     * Le token JWT est absent du header Authorization</li>
     * Le token JWT est expiré</li>
     * Le token JWT est invalide ou malformé</li>
     *
     * @param request La requête HTTP entrante contenant les détails de la tentative d'accès.
     *                Utilisé pour extraire l'URI, la méthode HTTP et le contexte de la requête.
     * @param response La réponse HTTP sortante où sera écrite la réponse d'erreur JSON.
     *                 Le statut HTTP sera défini à 401 et le content-type à application/json.
     * @param authException L'exception d'authentification qui a déclenché ce handler.
     *                      Contient la raison de l'échec d'authentification (ex: "Full authentication is required").
     *                      Cette information est loggée mais PAS exposée au client pour des raisons de sécurité.
     *
     * @throws IOException Si une erreur survient lors de l'écriture de la réponse JSON
     *                     dans le flux de sortie HTTP. Cette exception est propagée
     *                     et sera gérée par le conteneur servlet.
     *
     * @see AuthenticationException
     * @see HttpServletRequest
     * @see HttpServletResponse
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // Extraction des informations de la requête
        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();

        // Logging de la tentative d'accès non autorisée
        // Note : authException.getMessage() n'est PAS exposé au client pour la sécurité
        log.warn("Unauthorized access attempt: {} {} - Reason: {}",
                method, requestURI, authException.getMessage());

        // Récupération du message d'erreur localisé
        String errorMessage = messageService.getMessage("api.error.auth.required");

        // Construction de la réponse d'erreur standardisée
        ApiResponse<Void> errorResponse = ApiResponse.<Void>unauthorized(
                ErrorCodes.AUTHENTICATION_REQUIRED,  // Code d'erreur métier
                errorMessage,                        // Message localisé
                requestURI                           // Chemin de l'API
        );

        // Configuration de la réponse HTTP
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Sérialisation et écriture de la réponse JSON
        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );
    }
}