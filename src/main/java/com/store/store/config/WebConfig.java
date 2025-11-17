package com.store.store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Kardigué
 * @version 5.0 - Production Ready - UNIFIED
 * @since 2025-01-06
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {


    @Value("${store.file.directory}")
    private String uploadDir;


    // CONFIGURATION DES RESOURCE HANDLERS
    /**
     * GET http://localhost:8080/uploads/products/main/product_550e8400.jpg
     *   → Fichier : ~/eazystore-uploads/products/main/product_550e8400.jpg
     *
     * @param registry Registre pour configurer les ResourceHandlers
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String fileLocation = "file:" + absolutePath + "/";

        // Configuration SIMPLE et EFFICACE
        registry.addResourceHandler("/uploads/**").addResourceLocations(fileLocation).setCachePeriod(3600);

        log.info(" Handler configured: /uploads/** → {}", fileLocation);
    }

    // CONFIGURATION CORS
    /**
     * Configure les règles CORS pour l'application.
     *Configuration actuelle : Permissive (développement)
     * Restreindre les origines autorisées via {@code store.cors.allowed-origins}
     * @param registry Registre pour configurer les règles CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // À restreindre en production
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);

        log.info("CORS configured (permissive mode for development)");
    }
}