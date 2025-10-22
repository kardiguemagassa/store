package com.store.store.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Slf4j
@DisplayName("GlobalExceptionHandler Integration Tests")
class GlobalExceptionHandlerIntegrationTest extends GlobalExceptionHandlerTestConfig {

    @Configuration
    @RestController
    @RequestMapping("/api/test")
    static class TestController {

        @GetMapping("/resource-not-found")
        String triggerResourceNotFound() {
            throw new ResourceNotFoundException("Commande", "id", "12345");
        }

        @GetMapping("/business-error")
        String triggerBusinessError() {
            throw new BusinessException("Erreur métier de test");
        }

        @GetMapping("/access-denied")
        String triggerAccessDenied() {
            throw new AccessDeniedException("Accès refusé pour l'utilisateur");
        }

        @PostMapping("/validation")
        String triggerValidation(@RequestBody Map<String, String> request) {
            Map<String, String> errors = Map.of(
                    "email", "doit être une adresse email valide"
            );
            throw new ValidationException(errors);
        }

        @GetMapping("/global-error")
        String triggerGlobalError() {
            throw new RuntimeException("Erreur système inattendue");
        }

        @GetMapping("/single-validation")
        String triggerSingleValidation() {
            throw new ValidationException("email", "doit être valide");
        }
    }

    @Nested
    @DisplayName("HTTP Response Tests")
    class HttpResponseTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/test/resource-not-found → 404 Not Found")
        void whenResourceNotFound_thenReturn404() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/test/resource-not-found")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.status").value("Not Found"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").value("/api/test/resource-not-found"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/test/business-error → 400 Bad Request")
        void whenBusinessError_thenReturn400() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/test/business-error")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.status").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Erreur métier de test"))
                    .andExpect(jsonPath("$.path").value("/api/test/business-error"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/test/access-denied → 403 Forbidden")
        void whenAccessDenied_thenReturn403() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/test/access-denied")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.status").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value("Accès refusé"))
                    .andExpect(jsonPath("$.path").value("/api/test/access-denied"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /api/test/validation → 400 with validation errors")
        void whenValidationError_thenReturn400WithErrors() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"test\": \"data\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.errors.email").value("doit être une adresse email valide"))
                    .andExpect(jsonPath("$.path").value("/api/test/validation"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/test/global-error → 500 Internal Server Error")
        void whenGlobalError_thenReturn500WithTraceId() throws Exception {
            // When & Then
            ResultActions result = mockMvc.perform(get("/api/test/global-error")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.statusCode").value(500))
                    .andExpect(jsonPath("$.status").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("Une erreur technique s'est produite"))
                    .andExpect(jsonPath("$.traceId").exists())
                    .andExpect(jsonPath("$.path").value("/api/test/global-error"));

            // Log pour debug
            result.andDo(mvcResult -> {
                String response = mvcResult.getResponse().getContentAsString();
                log.debug("Error response: {}", response);
            });
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/test/single-validation → 400 with single field error")
        void whenSingleValidationError_thenReturn400WithSingleError() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/test/single-validation")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.errors").exists())
                    .andExpect(jsonPath("$.errors.email").value("doit être valide"))
                    .andExpect(jsonPath("$.path").value("/api/test/single-validation"));
        }
    }
}