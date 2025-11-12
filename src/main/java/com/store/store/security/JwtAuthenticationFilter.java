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
 * JwtAuthenticationFilter est une implémentation personnalisée de OncePerRequestFilter chargée
 * de valider les jetons JWT et de configurer le contexte de sécurité de l'utilisateur authentifié.
 * Il garantit que seuls les utilisateurs autorisés peuvent accéder aux ressources protégées, tout en ignorant
 * les chemins publics spécifiés.
 * Ce filtre fonctionne comme suit :
 * 1. Il extrait le JWT de l'en-tête Authorization des requêtes entrantes.
 * 2. Il valide la signature et l'expiration du JWT.
 * 3. Il charge les informations de l'utilisateur depuis le UserDetailsService pour l'utilisateur authentifié.
 * 4. Il configure le SecurityContext si le jeton et les informations de l'utilisateur sont valides.
 * Les erreurs d'authentification, telles que l'expiration du jeton, un format invalide ou des problèmes de signature,
 * sont consignées et transmises à l'AuthenticationEntryPoint sans lever d'exceptions explicites.
 * Les chemins publics sont exclus du processus de validation du JWT afin d'optimiser les performances du filtre.
 * @auteur Kardigué
 * @version 3.0 - Prêt pour la production
 * @since 2025-10-27
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
     * FLUX:
     * 1. Extraire JWT → 2. Valider → 3. Utilisateur du chargeur → 4. Définir l'authentification
     *GESTION DES ERREURS:
     * - ExpiredJwtException: Token expiré → Avertissement de journal, continuez (401 via EntryPoint)
     * - SignatureException: Signature invalide → Erreur de journalisation, continuer
     * - MalformedJwtException: Format invalide → Erreur de journalisation, continuer
     * - UsernameNotFoundException: utilisateur supprimé → Erreur de journalisation, continuer
     * - Autres: erreur de journalisation, continuer
     * Remarque : On ne lance PAS d'exception ici car le AuthenticationEntryPoint
     * gérer les erreurs 401 de manière centralisée.
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
     * Détermine si la requête HTTP donnée doit ignorer le filtrage.
     * Cette méthode vérifie si l'URI de la requête correspond à un chemin public prédéfini
     * et ignore la validation JWT si c'est le cas.
     * @param request la requête HTTP en cours de traitement
     * @return true si la requête doit ignorer le filtrage et ne pas être traitée davantage, false sinon
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
     * Extrait le jeton JWT de la requête HTTP fournie.
     * La méthode vérifie la valeur de l'en-tête « Authorization» dans la requête, s'assure qu'il commence
     * par le préfixe attendu du jeton porteur et extrait le jeton de l'en-tête s'il est valide.
     * @param request la {@code HttpServletRequest} à partir de laquelle le jeton JWT doit être extrait.
     * Pour que l'extraction réussisse, elle doit contenir un en-tête «Authorization» avec un jeton porteur valide.
     * @return le jeton JWT extrait sous forme de {@code String}, ou {@code null} si l'en-tête est absent,
     * ne commence pas par le préfixe attendu ou est invalide pour une autre raison.
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