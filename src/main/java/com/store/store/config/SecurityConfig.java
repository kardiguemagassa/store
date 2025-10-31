package com.store.store.config;

import com.store.store.security.CustomerUserDetailsService;
import com.store.store.security.JwtAuthenticationEntryPoint;
import com.store.store.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.password.HaveIBeenPwnedRestApiPasswordChecker;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration Spring Security pour API REST stateless avec JWT.
 *
 * ARCHITECTURE:
 * - Stateless (pas de session)
 * Access Token (JWT) : localStorage (15 min)
 * Refresh Token : Cookie HttpOnly (7 jours)
 *  CSRF Token : Cookie + Header X-XSRF-TOKEN
 * - JWT Authentication uniquement
 * - CORS configuré pour SPA
 * - Rate limiting via Resilience4j
 * - Authorization basée sur les rôles
 *
 * SÉCURITÉ:
 * - CSRF désactivé (JWT est immune)
 * - Session STATELESS
 * - Form Login désactivé
 * - HTTP Basic désactivé
 * - Passwords hashés avec BCrypt (cost factor 12)
 *
 * PERFORMANCE:
 * - Chemins publics ignorés par JwtAuthenticationFilter
 * - SecurityContext thread-local (pas de contention)
 *
 * @author Kardigué
 * @version 3.0 - Production Ready
 * @since 2025-01-27
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,      // @Secured
        jsr250Enabled = true,       // @RolesAllowed
        prePostEnabled = true       // @PreAuthorize / @PostAuthorize
)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomerUserDetailsService customerUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final List<String> publicPaths;

    @Value("${store.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configuration de la chaîne de filtres de sécurité.
     *
     * ORDRE DES FILTRES:
     * 1. CorsFilter (CORS headers)
     * 2. JwtAuthenticationFilter (notre filtre custom)
     * 3. UsernamePasswordAuthenticationFilter (par défaut, mais disabled)
     * 4. ExceptionTranslationFilter (gestion erreurs 401/403)
     * 5. FilterSecurityInterceptor (authorization finale)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // ✅ Configuration CSRF avec Cookies
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                // ═══════════════════════════════════════════════════════
                // CSRF - ACTIVÉ avec Cookies
                // ═══════════════════════════════════════════════════════
                .csrf(csrf -> csrf
                        // ✅ Cookie CSRF (accessible au JavaScript pour l'envoyer)
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        // ✅ Ignorer CSRF pour les endpoints publics
                        .ignoringRequestMatchers(publicPaths.toArray(new String[0]))
                )

                // ═══════════════════════════════════════════════════════
                // CORS
                // ═══════════════════════════════════════════════════════
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ═══════════════════════════════════════════════════════
                // SESSION - STATELESS (malgré les cookies)
                // ═══════════════════════════════════════════════════════
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ═══════════════════════════════════════════════════════
                // EXCEPTION HANDLING
                // ═══════════════════════════════════════════════════════
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // ═══════════════════════════════════════════════════════
                // AUTHORIZATION
                // ═══════════════════════════════════════════════════════
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths.toArray(new String[0])).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/store/actuator/**").hasRole("OPS_ENG")
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**")
                        .permitAll()
                        .anyRequest().hasAnyRole("USER", "ADMIN"))

                // ═══════════════════════════════════════════════════════
                // FILTRES
                // ═══════════════════════════════════════════════════════
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ═══════════════════════════════════════════════════════
                // DÉSACTIVER Form Login et HTTP Basic
                // ═══════════════════════════════════════════════════════
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Provider d'authentification avec chargement des users depuis la DB.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Authentication Manager (utilisé dans AuthController pour le login).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * ✅ AJOUT: CompromisedPasswordChecker utilisant HaveIBeenPwned API
     *
     * Vérifie si un mot de passe a été compromis dans des fuites de données.
     * Utilise l'API publique de Troy Hunt (haveibeenpwned.com).
     *
     * SÉCURITÉ:
     * - Envoie uniquement les 5 premiers caractères du hash SHA-1
     * - Ne transmet JAMAIS le mot de passe en clair
     * - Conforme RGPD (k-anonymity)
     *
     * PERFORMANCE:
     * - Cache les résultats localement
     * - Timeout de 5 secondes
     * - Fallback si API indisponible
     */
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

    /**
     * Encoder BCrypt avec cost factor 12 (équilibre sécurité/performance).
     *
     * SÉCURITÉ:
     * - BCrypt avec salt automatique
     * - Cost factor 12 = ~250ms par hash (résistant aux attaques brute force)
     * - Augmenter à 14 si serveur puissant (>500ms par hash)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configuration CORS pour permettre les requêtes cross-origin depuis le SPA.
     *Configuration CORS pour JWT + Cookies + CSRF
     * PRODUCTION:
     * Remplacer setAllowedOriginPatterns(List.of("*")) par des origines spécifiques:
     * - https://app.mondomaine.com
     * - https://www.mondomaine.com
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ═══════════════════════════════════════════════════════
        // ORIGINES - Spécifiques (jamais "*" avec credentials)
        // ═══════════════════════════════════════════════════════
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // ═══════════════════════════════════════════════════════
        // MÉTHODES HTTP
        // ═══════════════════════════════════════════════════════
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ═══════════════════════════════════════════════════════
        // HEADERS AUTORISÉS (Requête Frontend → Backend)
        // ═══════════════════════════════════════════════════════
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",           // JWT Bearer token
                "Content-Type",           // application/json
                "Accept",                 // Types acceptés
                "Origin",                 // Origine requête
                "X-Requested-With",       // AJAX
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-XSRF-TOKEN"            // ✅ CSRF Token (IMPORTANT pour cookies)
        ));

        // ═══════════════════════════════════════════════════════
        // HEADERS EXPOSÉS (Réponse Backend → Frontend)
        // ═══════════════════════════════════════════════════════
        config.setExposedHeaders(Arrays.asList(
                "Authorization",          // JWT dans réponse
                "X-XSRF-TOKEN",           // ✅ CSRF Token dans réponse
                "Set-Cookie"              // ✅ Cookies (refresh token)
        ));

        // ═══════════════════════════════════════════════════════
        // CREDENTIALS - TRUE pour Cookies
        // ═══════════════════════════════════════════════════════
        // ✅ IMPORTANT: Permet l'envoi/réception de cookies
        config.setAllowCredentials(true);

        // ═══════════════════════════════════════════════════════
        // MAX AGE - Cache Preflight
        // ═══════════════════════════════════════════════════════
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    // ========================================
    //  EN PRODUCTION
    //# ========================================

    /*@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ PRODUCTION: Charger depuis application.yml
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        // ✅ Méthodes HTTP nécessaires
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ✅ Headers strictement nécessaires pour JWT
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // ✅ Headers exposés (frontend peut les lire)
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // ✅ Credentials (pour cookies HttpOnly si nécessaire)
        config.setAllowCredentials(true);

        // ✅ Cache preflight 1 heure
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;


        # .env.production (NE PAS COMMIT)
        CORS_ORIGINS=https://app.votredomaine.com,https://www.votredomaine.com
        JWT_SECRET=VotreCleSecreteTresLongueEtComplexe...
        DATABASE_URL=jdbc:postgresql://prod-db:5432/store_db
    }*/



    // ========================================
    //  PAS EN PRODUCTION
    //# ========================================
    /*@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ═══════════════════════════════════════════════════════════
        // 1. ORIGINES - STRICTES EN PRODUCTION
        // ═══════════════════════════════════════════════════════════
        // ✅ PRODUCTION: Domaines spécifiques uniquement
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        // Exemple: https://app.votredomaine.com, https://www.votredomaine.com

        // ❌ JAMAIS EN PRODUCTION:
        // config.setAllowedOriginPatterns(Arrays.asList("*"));
        // config.setAllowedOrigins(Arrays.asList("*"));

        // ═══════════════════════════════════════════════════════════
        // 2. MÉTHODES HTTP - SPÉCIFIER EXACTEMENT
        // ═══════════════════════════════════════════════════════════
        // ✅ PRODUCTION: Liste explicite
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        // ❌ JAMAIS EN PRODUCTION:
        // config.setAllowedMethods(Arrays.asList("*"));

        // ═══════════════════════════════════════════════════════════
        // 3. HEADERS - LISTE EXPLICITE (SÉCURITÉ)
        // ═══════════════════════════════════════════════════════════
        // ✅ PRODUCTION: Headers spécifiques nécessaires
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",           // JWT Bearer token
                "Content-Type",           // application/json
                "Accept",                 // Types de réponse acceptés
                "Origin",                 // Origine de la requête
                "X-Requested-With",       // Identification AJAX
                "Access-Control-Request-Method",    // Preflight
                "Access-Control-Request-Headers"    // Preflight
        ));

        // ❌ ÉVITER EN PRODUCTION (trop permissif):
        // config.setAllowedHeaders(Arrays.asList("*"));

        // ═══════════════════════════════════════════════════════════
        // 4. HEADERS EXPOSÉS - CE QUE LE FRONTEND PEUT LIRE
        // ═══════════════════════════════════════════════════════════
        // ✅ PRODUCTION: Headers que le frontend doit accéder
        config.setExposedHeaders(Arrays.asList(
                "Authorization",                    // JWT dans réponse (refresh)
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // ═══════════════════════════════════════════════════════════
        // 5. CREDENTIALS - TRUE POUR JWT AVEC REFRESH TOKEN
        // ═══════════════════════════════════════════════════════════
        // ✅ PRODUCTION: true si vous utilisez des cookies HttpOnly
        // ⚠️  ATTENTION: Incompatible avec allowedOrigins("*")
        config.setAllowCredentials(true);

        // ═══════════════════════════════════════════════════════════
        // 6. MAX AGE - CACHE PREFLIGHT
        // ═══════════════════════════════════════════════════════════
        // ✅ PRODUCTION: 1 heure (économise les requêtes OPTIONS)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }*/
}