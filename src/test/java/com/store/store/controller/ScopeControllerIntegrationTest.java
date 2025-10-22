package com.store.store.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests d'Intégration - ScopeController")
class ScopeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Intégration - GET /api/v1/scope/application devrait fonctionner avec les vrais beans")
    @WithMockUser
    void integrationTest_ApplicationScope() throws Exception {
        // Premier appel
        mockMvc.perform(get("/api/v1/scope/application").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        // Deuxième appel - le compteur devrait s'incrémenter
        mockMvc.perform(get("/api/v1/scope/application").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("Intégration - GET /api/v1/scope/test devrait retourner le compteur actuel")
    @WithMockUser
    void integrationTest_TestEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/scope/test").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }
}