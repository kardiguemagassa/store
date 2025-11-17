package com.store.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration des chemins publics (sans authentification).
 * Les endpoints POST/PUT/DELETE restent protégés (ROLE_ADMIN requis).
 *
 * @author Kardigué
 * @version 3.1 - Security Fix
 * @since 2025-01-27
 */
@Configuration
public class PublicPathsConfig {

    @Bean
    public List<String> publicPaths() {
        return List.of(

                // ============================================
                // AUTHENTIFICATION (publics)
                // ============================================
                "/api/v1/auth/login",
                "/api/v1/auth/register",
                "/api/v1/auth/refresh",
                "/api/v1/auth/logout",
                "/api/v1/csrf-token",

                // ============================================
                // PRODUITS - LECTURE PUBLIQUE
                // ============================================
                "/api/v1/products",
                "/api/v1/products/search",
                "/api/v1/products/featured",
                "/api/v1/products/on-sale",
                "/api/v1/products/category/**",
                "/api/v1/products/*/image/bytes",

                // ============================================
                // CATÉGORIES - LECTURE PUBLIQUE
                // ============================================
                "/api/v1/categories",

                // ============================================
                // CONTACT (public)
                // ============================================
                "/api/v1/contacts/**",

                // ============================================
                // IMAGES UPLOADÉES (PUBLIC - CRITIQUE)
                // ============================================
                "/uploads/**",              // Tous les fichiers uploadés
                "/products/**",             // Images produits
                "/products/main/**",        // Images principales
                "/products/gallery/**",     // Images galerie

                // Si vos images sont dans un autre dossier :
                // "/static/images/**",
                // "/media/products/**",

                // ============================================
                // DOCUMENTATION API (public en dev)
                // ============================================
                "/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**",

                // ============================================
                // ACTUATOR (monitoring public)
                // ============================================
                "/store/actuator/health",
                "/store/actuator/health/**",
                "/store/actuator/info",

                // ============================================
                // ERREURS
                // ============================================
                "/error"
        );
    }
}