package com.store.store.config;

import com.store.store.dto.common.ApiResponse;
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
 * Gestion des erreurs
 * 401 Unauthorized → {@link JwtAuthenticationEntryPoint}
 * 403 Forbidden → {@link com.store.store.exception.GlobalExceptionHandler}
 *
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-01
 *
 * @see JwtAuthenticationFilter
 * @see JwtAuthenticationEntryPoint
 * @see CustomerUserDetailsService
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        securedEnabled = true,      // Active @Secured
        jsr250Enabled = true,       // Active @RolesAllowed
        prePostEnabled = true       // Active @PreAuthorize / @PostAuthorize
)
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Service de chargement des détails utilisateur depuis la BDD.
     * <p>Utilisé par le {@link DaoAuthenticationProvider} pour récupérer
     * les informations d'un utilisateur lors de l'authentification.
     */
    private final CustomerUserDetailsService customerUserDetailsService;

    /**
     * Filtre JWT qui intercepte chaque requête HTTP.
     * Responsabilités :
     * Extraire le token JWT depuis le cookie "accessToken"
     * Valider le token (signature, expiration, etc.)
     * Authentifier l'utilisateur dans le SecurityContext
     * Positionné AVANT {@link UsernamePasswordAuthenticationFilter}dans la chaîne de filtres.
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Point d'entrée pour gérer les erreurs d'authentification (401).
     * Déclenché automatiquement par Spring Security quand :
     * Aucun token JWT n'est présent
     * Le token JWT est expiré
     * Le token JWT est invalide
     * Retourne une réponse JSON standardisée {@link ApiResponse}
     * avec message localisé via {@link com.store.store.service.impl.MessageServiceImpl}.
     *
     * @see com.store.store.security.JwtAuthenticationEntryPoint#commence
     */
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Liste des chemins d'endpoints publics (sans authentification requise).
     * Injectée depuis {@link PublicPathsConfig} qui centralise
     * tous les endpoints publics de l'application.
     * Exemples : /api/v1/auth/**, /api/v1/products/**, /api/v1/contacts
     */
    private final List<String> publicPaths;

    /**
     * Origines CORS autorisées (depuis application.yml).
     * Exemple de configuration :
     * store:
     *   cors:
     *     allowed-origins: http://localhost:3000,http://localhost:4200
     * AMAIS utiliser "*" avec credentials:true !
     */
    @Value("${store.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configure la chaîne de filtres de sécurité Spring Security.
     * @param http Configuration de sécurité HTTP
     * @return Chaîne de filtres configurée
     * @throws Exception Si erreur de configuration
     *
     * @see JwtAuthenticationFilter
     * @see JwtAuthenticationEntryPoint
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // CSRF PROTECTION - Activé avec Cookies
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .csrf(csrf -> csrf
                        // Token CSRF dans un cookie (accessible au JavaScript)
                        // HttpOnly=false car le frontend doit lire le token pour l'envoyer
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        // Pas de CSRF pour les endpoints publics (GET sans modification d'état)
                        .ignoringRequestMatchers(publicPaths.toArray(new String[0]))
                )

                // CORS - Cross-Origin Resource Sharing
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // SESSION MANAGEMENT - Stateless
                // Pas de session côté serveur, l'état est dans le JWT
                // Les cookies sont juste un moyen de transport sécurisé
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // EXCEPTION HANDLING - Délégation
                // JwtAuthenticationEntryPoint gère automatiquement les erreurs 401
                // Il utilise ApiResponse + MessageService pour retourner JSON localisé
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // AUTHORIZATION RULES - Règles par endpoint
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics (sans authentification)
                        .requestMatchers(publicPaths.toArray(new String[0])).permitAll()

                        // Endpoints admin (ROLE_ADMIN requis)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Actuator (monitoring) - ROLE_ADMIN requis
                        .requestMatchers("/store/actuator/**").hasRole("ADMIN")

                        // Swagger UI - Public (développement)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**")
                        .permitAll()

                        // Tous les autres endpoints - Authentification requise
                        .anyRequest().hasAnyRole("USER", "ADMIN"))

                // AUTHENTICATION PROVIDER & FILTERS
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // DISABLE UNUSED FEATURES
                // Pas de formulaire de login HTML
                .formLogin(AbstractHttpConfigurer::disable)
                // Pas d'authentification HTTP Basic
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     *  Fournisseur d'authentification utilisant la base de données.
     *  {@link CustomerUserDetailsService} : Charge l'utilisateur depuis la BDD
     *  {@link BCryptPasswordEncoder} : Vérifie le mot de passe hashé
     *  Processus d'authentification
     *  UserDetailsService charge l'utilisateur par email
     *  PasswordEncoder compare le password fourni avec le hash en BDD
     *  Si match → Authentification réussie
     *  Sinon → BadCredentialsException (401)
     * @return Provider d'authentification configuré
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Gestionnaire d'authentification principal.
     *
     * <p>Utilisé dans {@link com.store.store.service.impl.AuthServiceImpl}
     * pour authentifier l'utilisateur lors du login.
     *
     * @param config Configuration d'authentification Spring
     * @return Manager d'authentification
     * @throws Exception Si erreur de configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Vérificateur de mots de passe compromis.
     * Utilise l'API "Have I Been Pwned" pour vérifier si un mot de passe a été exposé dans des fuites de données connues.
     * @return Vérificateur de mots de passe compromis
     * @see <a href="https://haveibeenpwned.com/API/v3">Have I Been Pwned API</a>
     */
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

    /**
     * Encodeur de mots de passe BCrypt.
     * Strength : 12 (bon compromis sécurité/performance)
     * @return Encodeur de mots de passe
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configuration CORS pour autoriser les requêtes cross-origin.
     * Le navigateur envoie automatiquement une requête OPTIONS avant
     * chaque requête cross-origin pour vérifier les autorisations.
     * Le cache de 3600s évite de répéter cette requête.
     *
     * @return Source de configuration CORS
     *
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">MDN CORS</a>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ORIGINES - Spécifiques (jamais "*" avec credentials)
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // MÉTHODES HTTP
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // HEADERS AUTORISÉS (Requête Frontend → Backend)
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",           // JWT Bearer token
                "Content-Type",           // application/json
                "Accept",                 // Types acceptés
                "Origin",                 // Origine requête
                "X-Requested-With",       // AJAX
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "X-XSRF-TOKEN"            // CSRF Token (IMPORTANT pour cookies)
        ));

        // HEADERS EXPOSÉS (Réponse Backend → Frontend)
        config.setExposedHeaders(Arrays.asList(
                "Authorization",          // JWT dans réponse
                "X-XSRF-TOKEN",           // CSRF Token dans réponse
                "Set-Cookie"              // Cookies (refresh token)
        ));

        // CREDENTIALS - TRUE pour Cookies
        // IMPORTANT : Permet l'envoi/réception de cookies (JWT, CSRF)
        config.setAllowCredentials(true);

        // MAX AGE - Cache Preflight
        // Cache la réponse OPTIONS pendant 1 heure (économise les requêtes)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}