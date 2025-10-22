package com.store.store.controller;

import com.store.store.config.TestSecurityConfig;
import com.store.store.scopes.ApplicationScopedBean;
import com.store.store.scopes.RequestScopedBean;
import com.store.store.scopes.SessionScopedBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScopeController.class)
@Import(TestSecurityConfig.class) // Assurez-vous d'avoir cette configuration de sécurité
@DisplayName("Tests Unitaires - ScopeController")
class ScopeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestScopedBean requestScopedBean;

    @MockitoBean
    private SessionScopedBean sessionScopedBean;

    @MockitoBean
    private ApplicationScopedBean applicationScopedBean;

    // ==================== TESTS REQUEST SCOPE ====================

    @Test
    @DisplayName("GET /api/v1/scope/request - Devrait retourner le nom d'utilisateur du scope request")
    @WithMockUser
    void testRequestScope_ShouldReturnUserName() throws Exception {
        // Given
        String expectedUserName = "John Doe";
        when(requestScopedBean.getUserName()).thenReturn(expectedUserName);

        // When & Then
        mockMvc.perform(get("/api/v1/scope/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUserName));

        verify(requestScopedBean, times(1)).setUserName("John Doe");
        verify(requestScopedBean, times(1)).getUserName();
    }

    @Test
    @DisplayName("GET /api/v1/scope/request - Devrait appeler les bonnes méthodes du bean request")
    @WithMockUser
    void testRequestScope_ShouldCallCorrectMethods() throws Exception {
        // Given
        when(requestScopedBean.getUserName()).thenReturn("John Doe");

        // When & Then
        mockMvc.perform(get("/api/v1/scope/request")
                        .with(csrf()))
                .andExpect(status().isOk());

        // Vérification des interactions
        verify(requestScopedBean, times(1)).setUserName("John Doe");
        verify(requestScopedBean, times(1)).getUserName();
        verifyNoMoreInteractions(requestScopedBean);
        verifyNoInteractions(sessionScopedBean, applicationScopedBean);
    }

    // ==================== TESTS SESSION SCOPE ====================

    @Test
    @DisplayName("GET /api/v1/scope/session - Devrait retourner le nom d'utilisateur du scope session")
    @WithMockUser
    void testSessionScope_ShouldReturnUserName() throws Exception {
        // Given
        String expectedUserName = "John Doe";
        when(sessionScopedBean.getUserName()).thenReturn(expectedUserName);

        // When & Then
        mockMvc.perform(get("/api/v1/scope/session")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUserName));

        verify(sessionScopedBean, times(1)).setUserName("John Doe");
        verify(sessionScopedBean, times(1)).getUserName();
    }

    @Test
    @DisplayName("GET /api/v1/scope/session - Devrait maintenir l'état de session entre les appels")
    @WithMockUser
    void testSessionScope_ShouldMaintainState() throws Exception {
        // Given
        when(sessionScopedBean.getUserName()).thenReturn("John Doe");

        // Premier appel
        mockMvc.perform(get("/api/v1/scope/session")
                        .with(csrf()))
                .andExpect(status().isOk());

        // Deuxième appel - même session
        mockMvc.perform(get("/api/v1/scope/session")
                        .with(csrf()))
                .andExpect(status().isOk());

        // Vérification que setUserName n'est appelé qu'une fois
        verify(sessionScopedBean, times(2)).setUserName("John Doe");
        verify(sessionScopedBean, times(2)).getUserName();
    }

    // ==================== TESTS APPLICATION SCOPE ====================

    @Test
    @DisplayName("GET /api/v1/scope/application - Devrait incrémenter et retourner le compteur")
    @WithMockUser
    void testApplicationScope_ShouldIncrementAndReturnCount() throws Exception {
        // Given
        int expectedCount = 5;
        when(applicationScopedBean.getVisitorCount()).thenReturn(expectedCount);

        // When & Then
        mockMvc.perform(get("/api/v1/scope/application")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(expectedCount)));

        verify(applicationScopedBean, times(1)).incrementVisitorCount();
        verify(applicationScopedBean, times(1)).getVisitorCount();
    }

    @Test
    @DisplayName("GET /api/v1/scope/application - Devrait partager l'état entre les requêtes")
    @WithMockUser
    void testApplicationScope_ShouldShareState() throws Exception {
        // Given - Simuler un compteur qui s'incrémente
        when(applicationScopedBean.getVisitorCount())
                .thenReturn(1)  // Premier appel
                .thenReturn(2)  // Deuxième appel
                .thenReturn(3); // Troisième appel

        // Appels multiples
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(get("/api/v1/scope/application")
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(String.valueOf(i)));
        }

        // Vérification
        verify(applicationScopedBean, times(3)).incrementVisitorCount();
        verify(applicationScopedBean, times(3)).getVisitorCount();
    }

    // ==================== TESTS ENDPOINT /test ====================

    @Test
    @DisplayName("GET /api/v1/scope/test - Devrait retourner le compteur sans l'incrémenter")
    @WithMockUser
    void testScopeEndpoint_ShouldReturnCountWithoutIncrementing() throws Exception {
        // Given
        int expectedCount = 42;
        when(applicationScopedBean.getVisitorCount()).thenReturn(expectedCount);

        // When & Then
        mockMvc.perform(get("/api/v1/scope/test")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(expectedCount)));

        verify(applicationScopedBean, times(1)).getVisitorCount();
        verify(applicationScopedBean, never()).incrementVisitorCount();
        verifyNoInteractions(requestScopedBean, sessionScopedBean);
    }

    // ==================== TESTS DE SÉCURITÉ ====================

    @Test
    @DisplayName("GET /api/v1/scope/request - Devrait échouer sans authentification")
    void testRequestScope_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/scope/request")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(requestScopedBean);
    }

    @Test
    @DisplayName("GET /api/v1/scope/session - Devrait échouer sans authentification")
    void testSessionScope_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/scope/session")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(sessionScopedBean);
    }

    @Test
    @DisplayName("GET /api/v1/scope/application - Devrait échouer sans authentification")
    void testApplicationScope_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/scope/application")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(applicationScopedBean);
    }

    // ==================== TESTS DE PERFORMANCE ET COMPORTEMENT ====================

    @Test
    @DisplayName("GET /api/v1/scope/application - Devrait retourner un entier valide")
    @WithMockUser
    void testApplicationScope_ShouldReturnValidInteger() throws Exception {
        // Given
        when(applicationScopedBean.getVisitorCount()).thenReturn(100);

        // When & Then
        mockMvc.perform(get("/api/v1/scope/application")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/scope/test - Devrait retourner un entier valide")
    @WithMockUser
    void testScopeEndpoint_ShouldReturnValidInteger() throws Exception {
        // Given
        when(applicationScopedBean.getVisitorCount()).thenReturn(50);

        // When & Then
        mockMvc.perform(get("/api/v1/scope/test")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isNumber())
                .andExpect(jsonPath("$").value(50));
    }

    // ==================== TESTS DE SÉPARATION DES SCOPES ====================

    @Test
    @DisplayName("Différents endpoints ne doivent pas interférer entre eux")
    @WithMockUser
    void differentEndpoints_ShouldNotInterfere() throws Exception {
        // Given
        when(requestScopedBean.getUserName()).thenReturn("Request User");
        when(sessionScopedBean.getUserName()).thenReturn("Session User");
        when(applicationScopedBean.getVisitorCount()).thenReturn(999);

        // When & Then - Appel des 3 endpoints
        mockMvc.perform(get("/api/v1/scope/request").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Request User"));

        mockMvc.perform(get("/api/v1/scope/session").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Session User"));

        mockMvc.perform(get("/api/v1/scope/application").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("999"));

        // Vérification que chaque bean est utilisé indépendamment
        verify(requestScopedBean, times(1)).setUserName("John Doe");
        verify(requestScopedBean, times(1)).getUserName();

        verify(sessionScopedBean, times(1)).setUserName("John Doe");
        verify(sessionScopedBean, times(1)).getUserName();

        verify(applicationScopedBean, times(1)).incrementVisitorCount();
        verify(applicationScopedBean, times(1)).getVisitorCount();
    }
}