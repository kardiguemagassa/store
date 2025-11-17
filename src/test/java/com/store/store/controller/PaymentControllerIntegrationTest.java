package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.payment.PaymentIntentRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "stripe.secret.key=sk_test_votre_cle_ici",
        "stripe.public.key=pk_test_votre_cle_publique_ici"
})
@DisplayName("Tests d'Intégration RÉELLE - PaymentController avec Stripe")
class PaymentControllerRealIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("INTÉGRATION RÉELLE - POST /create-payment-intent avec Stripe")
    @WithMockUser(roles = "USER")
    void realIntegration_CreatePaymentIntent_WithRealStripe_ShouldSucceed() throws Exception {
        // ARRANGE - Utilise un montant valide pour Stripe (minimum 0.50€ = 50 cents)
        PaymentIntentRequestDto requestDto = new PaymentIntentRequestDto(5000L, "eur"); // 50.00 EUR
        String requestJson = objectMapper.writeValueAsString(requestDto);

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/payment/create-payment-intent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(result -> {
                    System.out.println("=== STRIPE REAL INTEGRATION TEST ===");
                    System.out.println("Status: " + result.getResponse().getStatus());
                    System.out.println("Response: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.clientSecret").isString())
                .andExpect(jsonPath("$.clientSecret").isNotEmpty());
    }
}