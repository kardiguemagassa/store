package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.dto.order.OrderRequestDto;
import com.store.store.dto.order.OrderResponseDto;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.service.IOrderService;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Unitaires pour OrderController
 * Utilise des mocks pour isoler le controller de la couche service
 */
@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests Unitaires - OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IOrderService orderService;

    // =====================================================
    // TESTS POST /orders
    // =====================================================

    @Test
    @DisplayName("POST /orders - Succès")
    @WithMockUser(username = "test@example.com")
    void createOrder_Success() throws Exception {
        // Given
        OrderRequestDto request = TestDataBuilder.createValidOrderRequest();
        doNothing().when(orderService).createOrder(any());

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.statusCode").value("200"));

        verify(orderService, times(1)).createOrder(any());
    }

    @Test
    @DisplayName("POST /orders - Données invalides")
    @WithMockUser
    void createOrder_InvalidData_ReturnsBadRequest() throws Exception {
        // Given - Requête invalide
        OrderRequestDto invalidRequest = TestDataBuilder.createInvalidOrderRequest();

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());

        // Verify - Le service ne doit pas être appelé
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /orders - JSON malformé")
    @WithMockUser
    void createOrder_MalformedJson_ReturnsBadRequest() throws Exception {
        // Given
        String malformedJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("POST /orders - Produit non trouvé")
    @WithMockUser
    void createOrder_ProductNotFound_ReturnsNotFound() throws Exception {
        // Given
        OrderRequestDto request = TestDataBuilder.createValidOrderRequest();
        doThrow(new ResourceNotFoundException("Product", "id", "9999"))
                .when(orderService).createOrder(any());

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        verify(orderService, times(1)).createOrder(any());
    }

    @Test
    @DisplayName("POST /orders - Utilisateur non authentifié")
    void createOrder_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Given
        OrderRequestDto request = TestDataBuilder.createValidOrderRequest();

        // When & Then - Pas de @WithMockUser
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    // =====================================================
    // TESTS GET /orders
    // =====================================================

    @Test
    @DisplayName("GET /orders - Retourne les commandes du client")
    @WithMockUser(username = "test@example.com")
    void getCustomerOrders_Success() throws Exception {
        // Given
        List<OrderResponseDto> orders = List.of(
                TestDataBuilder.createOrderResponse(1L),
                TestDataBuilder.createOrderResponse(2L)
        );
        when(orderService.getCustomerOrders()).thenReturn(orders);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].orderId").value(1))
                .andExpect(jsonPath("$[1].orderId").value(2));

        verify(orderService, times(1)).getCustomerOrders();
    }

    @Test
    @DisplayName("GET /orders - Aucune commande")
    @WithMockUser
    void getCustomerOrders_EmptyList_ReturnsEmptyArray() throws Exception {
        // Given
        when(orderService.getCustomerOrders()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(orderService, times(1)).getCustomerOrders();
    }

    @Test
    @DisplayName("GET /orders - Utilisateur non authentifié")
    void getCustomerOrders_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // When & Then - Pas de @WithMockUser
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    // =====================================================
    // TESTS DE VALIDATION
    // =====================================================

    @Test
    @DisplayName("Validation : Prix total négatif")
    @WithMockUser
    void createOrder_NegativeTotalPrice_ReturnsBadRequest() throws Exception {
        // Given
        String invalidJson = """
            {
                "totalPrice": -99.99,
                "paymentIntentId": "pi_test",
                "paymentStatus": "paid",
                "items": [
                    {
                        "productId": 1,
                        "quantity": 1,
                        "price": 49.99
                    }
                ]
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Validation : Quantité négative")
    @WithMockUser
    void createOrder_NegativeQuantity_ReturnsBadRequest() throws Exception {
        // Given
        String invalidJson = """
            {
                "totalPrice": 99.99,
                "paymentIntentId": "pi_test",
                "paymentStatus": "paid",
                "items": [
                    {
                        "productId": 1,
                        "quantity": -1,
                        "price": 49.99
                    }
                ]
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Validation : Champs null")
    @WithMockUser
    void createOrder_NullFields_ReturnsBadRequest() throws Exception {
        // Given
        String invalidJson = """
            {
                "totalPrice": null,
                "paymentIntentId": null,
                "paymentStatus": null,
                "items": null
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());

        verifyNoInteractions(orderService);
    }
}