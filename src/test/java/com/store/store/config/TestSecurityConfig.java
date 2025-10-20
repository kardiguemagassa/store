package com.store.store.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de sécurité simplifiée pour les tests de contrôleurs.
 * Cette configuration reproduit les règles de sécurité sans les dépendances externes.
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)  // Désactiver CSRF pour simplifier les tests
                .authorizeHttpRequests(auth -> auth

                        // Endpoints publics (authentification)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/contacts").permitAll()

                        // Endpoints protégés
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Toutes les autres requêtes nécessitent l'authentification
                        .anyRequest().authenticated()
                )
                // Activer HTTP Basic pour les tests
                .httpBasic(basic -> {});

        return http.build();
    }
}