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

/**
 * An implementation of {@link AuthenticationEntryPoint} that handles unauthorized
 * access attempts for requests requiring authentication in a JWT-based security context.
 * It responds with a standard JSON error payload when the authentication process fails.
 * Primarily used in a Spring Security configuration.
 *
 * Responsibilities include:
 * - Logging unauthorized access attempts with details such as HTTP method, request URI,
 *   and the cause of the failure.
 * - Returning an HTTP 401 Unauthorized response with a structured JSON error body.
 *
 * This class relies on {@link ObjectMapper} to serialize the error response payload.
 *
 * Key functionality:
 * - Logs unauthorized access attempts to aid in debugging and audit trails.
 * - Constructs and sends a standardized error response using {@link ErrorResponseDto}.
 *
 * Constructor dependencies:
 * - {@link ObjectMapper}: Used for serializing the error response object into a JSON format.
 *
 * Implements method:
 * - {@code commence(HttpServletRequest request, HttpServletResponse response,
 *   AuthenticationException authException)}: Triggered whenever an unauthenticated user
 *   attempts to access a protected resource.
 *
 *   @author Kardigué
 *   @version 3.0
 *  @since 2025-10-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * A thread-safe utility for serializing and deserializing JSON data.
     *
     * The ObjectMapper instance is used to handle the conversion between Java objects
     * and their JSON representations. It is preconfigured and immutable to ensure
     * proper JSON processing throughout the application's lifecycle.
     *
     * Typically used in scenarios where JSON data needs to be read from or written to streams,
     * files, or HTTP responses within security-related workflows, such as in an authentication entry point.
     */
    private final ObjectMapper objectMapper;

    /**
     * Handles unauthorized access attempts by logging the details and responding with a
     * standardized JSON error response.
     *
     * This method is invoked when an unauthenticated user tries to access a resource
     * requiring authentication within the security context.
     *
     * @param request the HttpServletRequest object representing the incoming request. Provides details
     *                such as the URI and HTTP method of the request.
     * @param response the HttpServletResponse object used to construct the HTTP response. This is
     *                 utilized to send the 401 status and the JSON error message.
     * @param authException the AuthenticationException that describes the reason for the denial. Used
     *                      to log the cause of the unauthorized access attempt.
     * @throws IOException if an input/output error occurs while writing the error response to the HTTP
     *                     response output stream.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

        final String requestURI = request.getRequestURI();
        final String method = request.getMethod();

        log.warn("Unauthorized access attempt: {} {} - Reason: {}",
                method, requestURI, authException.getMessage());

        // Construire la réponse d'erreur standardisée
        ErrorResponseDto errorResponse = ErrorResponseDto.unauthorized(
                "AUTHENTICATION_REQUIRED",                  // errorCode
                "Authentication required to access this resource",  // message
                requestURI                                          // path
        );

        // Configurer la réponse HTTP
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Écrire la réponse JSON
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}