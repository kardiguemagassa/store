package com.store.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
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

                // UPLOADS
                "/uploads/**",
                "/uploads/products/**",
                "/uploads/categories/**",

                //ERREURS
                "/error"
        );
    }
}