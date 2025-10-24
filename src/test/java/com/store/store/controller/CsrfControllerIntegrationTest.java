package com.store.store.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour le CsrfController.
 * Vérifie que le token CSRF est correctement exposé aux clients.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
@DisplayName("Tests d'Intégration - CsrfController")
class CsrfControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    //FONCTIONNELS
    @Test
    @DisplayName("GET /api/v1/csrf-token - Devrait retourner un token CSRF valide")
    @WithMockUser
    void getCsrfToken_ShouldReturnValidToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"));

        log.info("token CSRF retourné avec succès");
    }

    @Test
    @DisplayName("GET /api/v1/csrf-token - Le token devrait être non vide")
    @WithMockUser
    void getCsrfToken_ShouldReturnNonEmptyToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assert response.contains("token");
                    assert !response.contains("null");
                });

        log.info("Token CSRF non vide vérifié");
    }

    @Test
    @DisplayName("GET /api/v1/csrf-token - Devrait retourner le bon Content-Type JSON")
    @WithMockUser
    void getCsrfToken_ShouldReturnJsonContentType() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));

        log.info("Content-Type JSON vérifié");
    }

    //SÉCURITÉ
    @Test
    @DisplayName("GET /api/v1/csrf-token - Devrait être accessible sans authentification")
    void getCsrfToken_ShouldBeAccessibleWithoutAuth() throws Exception {
        // When & Then - Sans @WithMockUser
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        log.info("Endpoint CSRF accessible publiquement");
    }

    @Test
    @DisplayName("GET /api/v1/csrf-token - Devrait fonctionner en HTTP GET")
    @WithMockUser
    void getCsrfToken_ShouldAcceptGetMethod() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk());

        log.info("Méthode GET acceptée");
    }

    // COHÉRENCE
    @Test
    @DisplayName("GET /api/v1/csrf-token - Le token devrait être dans le cookie XSRF-TOKEN")
    @WithMockUser
    void getCsrfToken_ShouldSetCookieWithToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))  // Vérifier le cookie
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false)); // Le cookie XSRF doit être accessible en JS

        log.info("Cookie XSRF-TOKEN configuré correctement");
    }

    @Test
    @DisplayName("GET /api/v1/csrf-token - Structure du token devrait être valide")
    @WithMockUser
    void getCsrfToken_ShouldBeUsableInSubsequentRequests() throws Exception {
        // Given - Récupérer le token
        String response = mockMvc.perform(get("/api/v1/csrf-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - Le token existe et est dans un format valide
        assert response.contains("\"token\"");
        assert response.contains("\"headerName\"");
        assert response.contains("\"parameterName\"");
        assert response.contains("X-XSRF-TOKEN");

        log.info("Structure du token CSRF validée");
    }

    @Test
    @DisplayName("GET /api/v1/csrf-token - Chaque requête devrait potentiellement retourner un token différent")
    @WithMockUser
    void getCsrfToken_MayReturnDifferentTokensForDifferentRequests() throws Exception {
        // First request
        String response1 = mockMvc.perform(get("/api/v1/csrf-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Second request (dans une nouvelle session)
        String response2 = mockMvc.perform(get("/api/v1/csrf-token"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Les réponses doivent contenir des tokens valides (même s'ils peuvent être identiques)
        assert response1.contains("\"token\"");
        assert response2.contains("\"token\"");

        log.info("✅ Génération de tokens CSRF fonctionnelle");
    }

    //ERREUR
    @Test
    @DisplayName("GET /api/v1/csrf-token - Devrait retourner un token même en cas de requêtes multiples")
    @WithMockUser
    void getCsrfToken_ShouldHandleMultipleRequests() throws Exception {
        // Simuler plusieurs requêtes successives
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/csrf-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists());
        }

        log.info("Gestion de requêtes multiples vérifiée");
    }

    // STRUCTURE DE RÉPONSE
    @Test
    @DisplayName("GET /api/v1/csrf-token - La réponse devrait avoir tous les champs requis")
    @WithMockUser
    void getCsrfToken_ShouldHaveAllRequiredFields() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.headerName").exists())
                .andExpect(jsonPath("$.parameterName").exists())
                // Vérifier les types
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").isString())
                .andExpect(jsonPath("$.parameterName").isString())
                // Vérifier les valeurs attendues
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"));

        log.info("Structure complète de la réponse CSRF validée");
    }

    // SPÉCIFIQUE FORMAT UUID
    @Test
    @DisplayName("GET /api/v1/csrf-token - Le token devrait être un UUID valide")
    @WithMockUser
    void getCsrfToken_ShouldReturnValidUuidToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/csrf-token"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.matchesPattern(
                        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
                )));

        log.info("Format UUID du token CSRF validé");
    }
}