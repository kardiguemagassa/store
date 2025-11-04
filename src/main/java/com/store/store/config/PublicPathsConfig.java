package com.store.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for defining public paths within the application.
 *
 * This class registers a bean that provides a list of URL patterns that are
 * publicly accessible without authentication. The paths listed within this bean
 * typically include endpoints for authentication, public-facing business use cases,
 * API documentation, public monitoring (actuator), and error handling.
 *
 * The `/api/v1/products/**` and `/api/v1/contacts/**` endpoints represent
 * business logic for publicly accessible product viewing and contact forms, respectively.
 *
 * For API documentation purposes, the paths like `/api-docs/**` and `/swagger-ui/**`
 * provide integration with Swagger/OpenAPI.
 *
 * Actuator endpoints for monitoring such as `/store/actuator/health` are included
 * as publicly accessible paths to allow health checks and status information for public usage.
 *
 * All paths specified in this configuration can typically bypass security filters
 * or authentication mechanisms and are exposed for general use.
 *
 * This configuration highlights multiple categories of public paths:
 * - Authentication: Includes paths for login, registration, token management, logout, etc.
 * - Public Business Logic: Paths related to product consultation and contact forms.
 * - API Documentation: Includes paths for Swagger UI and API documentation.
 * - Monitoring: Exposes public Actuator endpoints for monitoring services.
 * - Error Handling: Exposes the `/error` endpoint for handling and displaying errors.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-01-27
 */
@Configuration
public class PublicPathsConfig {

    @Bean
    public List<String> publicPaths() {
        return List.of(

                // AUTHENTIFICATION
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout",
                "/api/v1/csrf-token",


                // MÉTIER - LECTURE PUBLIQUE
                "/api/v1/products/**",      // Consultation produits
                "/api/v1/contacts/**",      // Formulaire contact

                // DOCUMENTATION API
                "/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",

                // ACTUATOR (monitoring public)
                "/store/actuator/health",
                "/store/actuator/health/**",
                "/store/actuator/info",


                //ERREURS
                "/error"
        );
    }
}