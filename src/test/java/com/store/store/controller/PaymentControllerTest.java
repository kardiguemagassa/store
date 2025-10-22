package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.dto.PaymentIntentRequestDto;
import com.store.store.dto.PaymentIntentResponseDto;
import com.store.store.service.IPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Unitaires pour PaymentController
 * Utilise des mocks pour isoler le controller du service
 */
@WebMvcTest(PaymentController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests Unitaires - PaymentController")
@Slf4j
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPaymentService paymentService;

    // ==================== TESTS POST /api/v1/payment/create-payment-intent ====================

    @Test
    @DisplayName("POST /create-payment-intent - Devrait créer un payment intent avec succès")
    @WithMockUser
    void createPaymentIntent_WithValidData_ShouldReturnClientSecret() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(10000L, "eur");
        PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto("pi_test_secret_123456");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("pi_test_secret_123456"));

        verify(paymentService, times(1)).createPaymentIntent(any(PaymentIntentRequestDto.class));
        log.info("✅ Test POST payment intent réussi");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait accepter différentes devises")
    @WithMockUser
    void createPaymentIntent_WithDifferentCurrencies_ShouldSucceed() throws Exception {
        // Given - EUR
        PaymentIntentRequestDto requestEur = new PaymentIntentRequestDto(5000L, "eur");
        PaymentIntentResponseDto responseEur = new PaymentIntentResponseDto("pi_eur_secret");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseEur);

        // When & Then - EUR
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestEur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("pi_eur_secret"));

        // Given - USD
        PaymentIntentRequestDto requestUsd = new PaymentIntentRequestDto(5000L, "usd");
        PaymentIntentResponseDto responseUsd = new PaymentIntentResponseDto("pi_usd_secret");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseUsd);

        // When & Then - USD
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestUsd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("pi_usd_secret"));

        log.info("✅ Différentes devises acceptées");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait accepter différents montants")
    @WithMockUser
    void createPaymentIntent_WithDifferentAmounts_ShouldSucceed() throws Exception {
        // Given
        PaymentIntentRequestDto smallAmount = new PaymentIntentRequestDto(100L, "eur");
        PaymentIntentRequestDto largeAmount = new PaymentIntentRequestDto(1000000L, "eur");

        PaymentIntentResponseDto response = new PaymentIntentResponseDto("pi_test_secret");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(response);

        // When & Then - Small amount
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(smallAmount)))
                .andExpect(status().isOk());

        // When & Then - Large amount
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(largeAmount)))
                .andExpect(status().isOk());

        verify(paymentService, times(2)).createPaymentIntent(any(PaymentIntentRequestDto.class));
        log.info("✅ Différents montants acceptés");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait gérer les erreurs du service")
    @WithMockUser
    void createPaymentIntent_WhenServiceFails_ShouldReturnError() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(10000L, "eur");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenThrow(new RuntimeException("Échec de paiement"));

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError());

        verify(paymentService, times(1)).createPaymentIntent(any(PaymentIntentRequestDto.class));
        log.info("✅ Gestion erreur service vérifiée");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait échouer sans authentification")
    void createPaymentIntent_WithoutAuth_ShouldReturnUnauthorized() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(10000L, "eur");
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
        log.info("✅ Sécurité POST payment intent vérifiée");
    }

    // ==================== TESTS DE VALIDATION     A REPPRENDE ICI ))))))))) ====================

    @Test
    @DisplayName("POST /create-payment-intent - Devrait échouer avec body vide")
    @WithMockUser
    void createPaymentIntent_WithEmptyBody_ShouldReturnBadRequest() throws Exception {
        // Given
        String emptyJson = "{}";

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists()) // ✅ Vérifie l'existence de "errors"
                .andExpect(jsonPath("$.errors.amount").value("Le montant ne peut pas être null"))
                .andExpect(jsonPath("$.errors.currency").value("La devise ne peut pas être vide"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Erreur de validation des données"));

        verifyNoInteractions(paymentService);
        log.info("✅ Body vide rejeté");
    }




    @Test
    @DisplayName("POST /create-payment-intent - Devrait échouer sans amount")
    @WithMockUser
    void createPaymentIntent_WithoutAmount_ShouldReturnBadRequest() throws Exception {
        // Given
        String jsonWithoutAmount = """
        {
            "currency": "eur"
        }
        """;

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutAmount))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait échouer sans currency")
    @WithMockUser
    void createPaymentIntent_WithoutCurrency_ShouldReturnBadRequest() throws Exception {
        // Given
        String jsonWithoutCurrency = """
        {
            "amount": 10000
        }
        """;

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutCurrency))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait rejeter une devise invalide")
    @WithMockUser
    void createPaymentIntent_WithInvalidCurrency_ShouldReturnBadRequest() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(10000L, "EURO"); // 4 lettres
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then - Doit être rejeté par la validation
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currency").value("La devise doit être un code ISO 4217 de 3 lettres"));

        verifyNoInteractions(paymentService);
        log.info("✅ Devise invalide correctement rejetée");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait échouer avec JSON malformé")
    @WithMockUser
    void createPaymentIntent_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Given
        String malformedJson = "{invalid json}";

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
        log.info("✅ JSON malformé rejeté");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait rejeter Content-Type invalide")
    @WithMockUser
    void createPaymentIntent_WithInvalidContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // Given
        String content = "invalid content";

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(content))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(paymentService);
        log.info("✅ Content-Type invalide rejeté");
    }

    // ==================== TESTS DE RÉPONSE ====================

    @Test
    @DisplayName("POST /create-payment-intent - Devrait retourner le bon Content-Type")
    @WithMockUser
    void createPaymentIntent_ShouldReturnJsonContentType() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(10000L, "eur");
        PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto("pi_test_secret");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        log.info("✅ Content-Type JSON vérifié");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait retourner la structure correcte")
    @WithMockUser
    void createPaymentIntent_ShouldReturnCorrectStructure() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(10000L, "eur");
        PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto("pi_test_secret_123456");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.clientSecret").isString())
                .andExpect(jsonPath("$.clientSecret").isNotEmpty());

        log.info("✅ Structure de réponse vérifiée");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait appeler le service avec les bonnes données")
    @WithMockUser
    void createPaymentIntent_ShouldCallServiceWithCorrectData() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(25000L, "eur");
        PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto("pi_test_secret");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then
        verify(paymentService, times(1)).createPaymentIntent(any(PaymentIntentRequestDto.class));
        verify(paymentService, only()).createPaymentIntent(any(PaymentIntentRequestDto.class));
        log.info("✅ Appel service avec données correctes vérifié");
    }

    // ==================== TESTS DE CAS LIMITES ====================

    @Test
    @DisplayName("POST /create-payment-intent - Devrait rejeter un montant zéro")
    @WithMockUser
    void createPaymentIntent_WithZeroAmount_ShouldReturnBadRequest() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(0L, "eur");
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then - Doit être rejeté par la validation
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("Le montant doit être d'au moins 50 centimes"));

        verifyNoInteractions(paymentService); // Le service ne doit pas être appelé
        log.info("✅ Montant zéro correctement rejeté");
    }

    // ==================== TESTS DE VALIDATION AVEC ERREURS ====================
    @Test
    @DisplayName("POST /create-payment-intent - Devrait rejeter un montant négatif")
    @WithMockUser
    void createPaymentIntent_WithNegativeAmount_ShouldReturnBadRequest() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(-1000L, "eur");
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then - Doit être rejeté par la validation
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("Le montant doit être d'au moins 50 centimes"));

        verifyNoInteractions(paymentService); // Le service ne doit pas être appelé
        log.info("✅ Montant négatif correctement rejeté");
    }

    @Test
    @DisplayName("POST /create-payment-intent - Devrait accepter le montant minimum (50)")
    @WithMockUser
    void createPaymentIntent_WithMinimumAmount_ShouldSucceed() throws Exception {
        // Given - Montant minimum de 50 centimes
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(50L, "eur");
        PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto("pi_test_secret");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk());

        verify(paymentService, times(1)).createPaymentIntent(any(PaymentIntentRequestDto.class));
        log.info("✅ Montant minimum accepté");
    }




   //==================================================================================================================

    @Test
    @DisplayName("POST /create-payment-intent - Le clientSecret devrait être non vide")
    @WithMockUser
    void createPaymentIntent_ShouldReturnNonEmptyClientSecret() throws Exception {
        // Given
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(10000L, "eur");
        PaymentIntentResponseDto responseDto = new PaymentIntentResponseDto("pi_1234567890_secret_abcdefghijklmnop");

        when(paymentService.createPaymentIntent(any(PaymentIntentRequestDto.class)))
                .thenReturn(responseDto);

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("pi_1234567890_secret_abcdefghijklmnop"))
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    assert !responseBody.contains("null");
                    assert !responseBody.contains("\"clientSecret\":\"\"");
                });

        log.info("✅ ClientSecret non vide vérifié");
    }
}