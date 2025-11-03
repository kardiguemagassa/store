package com.store.store.security;

import com.store.store.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JwtAuthenticationFilter is a custom implementation of OncePerRequestFilter responsible
 * for validating JWT tokens and setting up the authenticated user's security context.
 * It ensures that only authorized users can access protected resources, while ignoring
 * specified public paths.
 *
 * This filter operates as follows:
 * 1. Extracts the JWT from the Authorization header of incoming requests.
 * 2. Validates the JWT's signature and expiration.
 * 3. Loads user details from the UserDetailsService for the authenticated user.
 * 4. Sets up the SecurityContext if the token and user details are valid.
 *
 * Authentication errors such as token expiration, invalid format, or signature issues
 * are logged and passed to the AuthenticationEntryPoint without explicitly throwing exceptions.
 * Public paths are excluded from the JWT validation process to optimize the filter's performance.
 *
 * @author Kardigué
 *  @version 3.0 - Production Ready
 *  @since 2025-10-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final List<String> publicPaths;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    // AntPathMatcher pour supporter les patterns comme /api/v1/auth/**
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Filtre principal - Validation JWT et configuration du SecurityContext.
     *
     * FLUX:
     * 1. Extraire JWT → 2. Valider → 3. Charger User → 4. Set Authentication
     *
     * GESTION DES ERREURS:
     * - ExpiredJwtException: Token expiré → Log warning, continue (401 via EntryPoint)
     * - SignatureException: Signature invalide → Log error, continue
     * - MalformedJwtException: Format invalide → Log error, continue
     * - UsernameNotFoundException: User supprimé → Log error, continue
     * - Autres: Log error, continue
     *
     * Note: On ne lance PAS d'exception ici car le AuthenticationEntryPoint
     * gère les erreurs 401 de manière centralisée.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestPath = request.getRequestURI();
        final String jwt = extractJwtFromRequest(request);

        // Si pas de JWT, continuer la chaîne (Spring Security gérera l'authentification)
        if (jwt == null) {
            log.debug("No JWT found in request: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1: Valider JWT (signature + expiration)
            if (!jwtUtil.validateJwtToken(jwt)) {
                log.warn("Invalid JWT token for path: {}", requestPath);
                filterChain.doFilter(request, response);
                return;
            }

            // 2: Extraire le username (email)
            final String username = jwtUtil.getUsernameFromJwtToken(jwt);

            // 3: Vérifier si l'utilisateur n'est pas déjà authentifié
            // (Optimisation: éviter de recharger depuis la DB si déjà authentifié)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 4: Charger les détails complets depuis la DB
                // (roles, permissions, enabled status, etc.)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5: Créer l'objet Authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null, // Credentials = null (déjà authentifié via JWT)
                                userDetails.getAuthorities()
                        );

                // 6: Ajouter les détails de la requête (IP, session, etc.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7: Placer l'Authentication dans le SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT authentication successful for user: {} on path: {}",
                        username, requestPath);
            }

        } catch (ExpiredJwtException e) {
            // Token expiré - L'utilisateur doit utiliser le refresh token
            log.warn("JWT token expired for path: {} - User should refresh token", requestPath);
            // Ne pas bloquer la requête, laisser AuthenticationEntryPoint gérer le 401

        } catch (SignatureException e) {
            // Signature invalide - Token potentiellement modifié
            log.error("JWT signature validation failed for path: {} - Possible tampering attempt", requestPath);

        } catch (MalformedJwtException e) {
            // Format JWT invalide
            log.error("Malformed JWT token for path: {}", requestPath);

        } catch (UsernameNotFoundException e) {
            // User n'existe plus en DB (supprimé entre temps)
            log.error("User not found for JWT token on path: {} - User may have been deleted", requestPath);

        } catch (Exception e) {
            // Erreur inattendue
            log.error("Cannot set user authentication for path: {} - Error: {}", requestPath, e.getMessage(), e);
        }

        // TOUJOURS continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }

    /**
     * Determines whether the given HTTP request should bypass filtering.
     * This method checks if the request URI matches any predefined public paths
     * and skips JWT validation if it matches.
     *
     * @param request the HTTP request being processed
     * @return true if the request should bypass filtering and not be processed further, false otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final String path = request.getRequestURI();

        // Vérifier si le chemin correspond à un pattern public
        boolean isPublicPath = publicPaths.stream().anyMatch(publicPath -> pathMatcher.match(publicPath, path));

        if (isPublicPath) {
            log.trace("Public path detected, skipping JWT validation: {}", path);
        }

        return isPublicPath;
    }

    /**
     * Extracts the JWT token from the provided HTTP request.
     * The method checks the value of the "Authorization" header in the request, verifies that it starts
     * with the expected bearer token prefix, and extracts the token from the header if valid.
     *
     * @param request the {@code HttpServletRequest} from which the JWT token is to be extracted.
     *                It must contain an "Authorization" header with a valid bearer token to succeed.
     * @return the extracted JWT token as a {@code String}, or {@code null} if the header is missing,
     *         does not start with the expected prefix, or is otherwise invalid.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        final String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null &&
                bearerToken.startsWith(BEARER_PREFIX) &&
                bearerToken.length() > BEARER_PREFIX_LENGTH) {
            return bearerToken.substring(BEARER_PREFIX_LENGTH);
        }

        return null;
    }
}