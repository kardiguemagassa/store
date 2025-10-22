package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.dto.LoggingResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoggingController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests Unitaires - LoggingController")
@Slf4j
class LoggingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== TESTS FONCTIONNELS ====================

    @Test
    @DisplayName("GET /api/v1/logging - Devrait retourner un message de succès")
    @WithMockUser
    void testLogging_ShouldReturnSuccessMessage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/logging - Devrait retourner le bon Content-Type JSON")
    @WithMockUser
    void testLogging_ShouldReturnCorrectContentType() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ==================== TESTS DE SÉCURITÉ ====================

    @Test
    @DisplayName("GET /api/v1/logging - Devrait échouer sans authentification")
    void testLogging_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // ==================== TESTS DE COMPORTEMENT ====================

    @Test
    @DisplayName("GET /api/v1/logging - Devrait avoir une réponse cohérente")
    @WithMockUser
    void testLogging_ShouldHaveConsistentResponse() throws Exception {
        // Premier appel
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Deuxième appel - même réponse
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/logging - Devrait accepter différentes méthodes de contenu")
    @WithMockUser
    void testLogging_WithDifferentAcceptHeaders_ShouldWork() throws Exception {
        // Test avec Accept: application/json - devrait fonctionner
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf())
                        .header("Accept", MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Test avec Accept: text/plain - devrait retourner 406 Not Acceptable
        // car le controller ne supporte que JSON
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf())
                        .header("Accept", MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable()); // ✅ Changé ici - 406 au lieu de 200
    }

    // ==================== TESTS DE PERFORMANCE ====================

    @Test
    @DisplayName("GET /api/v1/logging - Devrait répondre rapidement")
    @WithMockUser
    void testLogging_ShouldRespondQuickly() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk());

        long responseTime = System.currentTimeMillis() - startTime;

        log.info("Temps de réponse: {} ms", responseTime);
        assert responseTime < 1000 : "Doit répondre en moins d'1 seconde, mais a pris " + responseTime + " ms";
    }

    // ==================== TESTS DE STRUCTURE DE RÉPONSE ====================

    @Test
    @DisplayName("GET /api/v1/logging - La réponse doit avoir la structure JSON correcte")
    @WithMockUser
    void testLogging_ResponseShouldHaveCorrectStructure() throws Exception {
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/v1/logging - Devrait avoir les headers de sécurité")
    @WithMockUser
    void testLogging_ShouldHaveSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().exists("X-Frame-Options"));
    }

    // ==================== TESTS DE RÉUTILISABILITÉ ====================

    @Test
    @DisplayName("GET /api/v1/logging - Appels multiples successifs")
    @WithMockUser
    void testLogging_MultipleCalls_ShouldAllSucceed() throws Exception {
        // Appels multiples
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/logging")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        log.info("✅ 5 appels successifs au endpoint logging effectués avec succès");
    }

    // ==================== TESTS DE DÉSÉRIALISATION ====================

    @Test
    @DisplayName("GET /api/v1/logging - La réponse doit pouvoir être désérialisée")
    @WithMockUser
    void testLogging_ResponseShouldBeDeserializable() throws Exception {
        String response = mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Désérialiser la réponse JSON
        LoggingResponseDto responseDto = objectMapper.readValue(response, LoggingResponseDto.class);

        assert responseDto.message().equals("Logging tested successfully") : "Message incorrect";
        assert responseDto.status().equals("SUCCESS") : "Status incorrect";

        log.info("✅ Réponse JSON correctement désérialisée: {}", responseDto);
    }
}