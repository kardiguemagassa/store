/*package com.store.store.controller;

import com.store.store.dto.RefreshTokenRequestDto;
import com.store.store.entity.RefreshToken;
import com.store.store.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.RequestEntity.post;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void refresh_ShouldReturnNewTokens_WhenRefreshTokenValid() {
        // Given: Un refresh token valide
        String refreshToken = "valid-token";

        // When: POST /refresh
        MockHttpServletRequestBuilder request = post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}");

        // Then: 200 OK avec nouveaux tokens
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void refresh_ShouldReturn401_WhenRefreshTokenInvalid() {
        // When: POST /refresh avec token invalide
        MockHttpServletRequestBuilder request = post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"invalid-token\"}");

        // Then: 401 Unauthorized
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_ShouldRevokeOldToken_WhenRefreshSuccessful() {
        // Given: Un refresh token valide
        RefreshToken oldToken = createValidRefreshToken();

        // When: POST /refresh
        refreshTokenController.refreshToken(
                new RefreshTokenRequestDto(oldToken.getToken())
        );

        // Then: L'ancien token est révoqué
        RefreshToken revokedToken = refreshTokenRepository.findByToken(
                oldToken.getToken()
        ).orElseThrow();

        assertTrue(revokedToken.isRevoked());
    }
}*/