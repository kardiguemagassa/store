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
 * The SecurityConfig class configures security settings for the application.
 * It enables Web security, method security, and sets up various components such as authentication,
 * authorization, CORS, CSRF protection, session management, and security filters.
 *
 * This configuration includes:
 * - Custom authentication and authorization settings.
 * - JWT token-based stateless authentication.
 * - Support for method-level security annotations (e.g., @Secured, @RolesAllowed).
 * - CORS configurations for handling cross-origin requests.
 * - CSRF protection using cookies.
 * - Customized exception handling mechanisms.
 *
 * @author Kardigué
 * @version 3.0 - Production Ready
 * @since 2025-10-27
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
     * Configures the security filter chain for the application.
     * This method sets up CSRF protection with cookies, CORS configuration, stateless session management,
     * exception handling, authentication providers, authorization rules, and disables form login and HTTP basic authentication.
     *
     * @param http the {@link HttpSecurity} object used to configure the security settings
     * @return the configured {@link SecurityFilterChain} for the application
     * @throws Exception if an error occurs during the configuration of the security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Configuration CSRF avec Cookies
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                // CSRF - ACTIVÉ avec Cookies
                .csrf(csrf -> csrf
                        // Cookie CSRF (accessible au JavaScript pour l'envoyer)
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler)
                        // Ignorer CSRF pour les endpoints publics
                        .ignoringRequestMatchers(publicPaths.toArray(new String[0]))
                )

                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // SESSION - STATELESS (malgré les cookies)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // EXCEPTION HANDLING
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // AUTHORIZATION
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths.toArray(new String[0])).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        //.requestMatchers("/store/actuator/**").hasRole("OPS_ENG")
                        .requestMatchers("/store/actuator/**").hasRole("ADMIN")
                        //http://localhost:8080/swagger-ui/index.html#/
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**")
                        .permitAll()
                        .anyRequest().hasAnyRole("USER", "ADMIN"))

                // FILTRES
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // DÉSACTIVER Form Login et HTTP Basic
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Configures and provides an AuthenticationProvider bean.
     * The configured provider uses a custom UserDetailsService and a BCrypt password encoder.
     *
     * @return an instance of {@link AuthenticationProvider} that handles user authentication logic
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Creates and provides an {@link AuthenticationManager} bean.
     * This method retrieves the {@link AuthenticationManager} from the provided {@link AuthenticationConfiguration}.
     *
     * @param config the {@link AuthenticationConfiguration} containing the authentication setup and manager
     * @return an instance of {@link AuthenticationManager} for handling authentication processes
     * @throws Exception if an error occurs during the retrieval of the {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides an instance of {@link CompromisedPasswordChecker}.
     * This bean implements a password checking mechanism that integrates
     * with the "Have I Been Pwned" API to determine whether a given password
     * has been compromised in known data breaches.
     *
     * @return an instance of {@link CompromisedPasswordChecker} for verifying compromised passwords
     */
    @Bean
    public CompromisedPasswordChecker compromisedPasswordChecker() {
        return new HaveIBeenPwnedRestApiPasswordChecker();
    }

    /**
     * Creates and provides a PasswordEncoder bean.
     * This bean sets up a BCryptPasswordEncoder with a specified strength for encoding passwords.
     *
     * @return an instance of {@link PasswordEncoder} configured with BCrypt hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configures the Cross-Origin Resource Sharing (CORS) settings for the application.
     * This method sets up allowed origins, HTTP methods, request headers, response headers,
     * credentials, and pre-flight request caching duration for handling cross-origin requests.
     *
     * @return an instance of {@link CorsConfigurationSource} with the configured CORS settings
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ORIGINES - Spécifiques (jamais "*" avec credentials)
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // METHODS HTTP
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
        // IMPORTANT: Permet l'envoi/réception de cookies
        config.setAllowCredentials(true);

        // MAX AGE - Cache Preflight // Cache preflight 1 heure (économise les requêtes OPTIONS)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}