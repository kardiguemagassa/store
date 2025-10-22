package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.config.TestSecurityConfig;
import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.OrderItemDto;
import com.store.store.dto.OrderRequestDto;
import com.store.store.dto.OrderResponseDto;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.service.IOrderService;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Unitaires pour OrderController
 * Utilise des mocks pour isoler le controller de la couche service
 */
@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests Unitaires - OrderController")
@Slf4j
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IOrderService iOrderService;

    private OrderRequestDto validOrderRequest;
    private List<OrderResponseDto> testOrderResponses;

    @BeforeEach
    void setUp() {
        // Créer une requête de commande valide
        List<OrderItemDto> items = List.of(
                new OrderItemDto(1L, 2, new BigDecimal("50.00")),
                new OrderItemDto(2L, 1, new BigDecimal("75.00"))
        );

        validOrderRequest = new OrderRequestDto(
                new BigDecimal("175.00"),
                "pi_test_123456",
                "paid",
                items
        );

        // Créer des réponses de test
        testOrderResponses = List.of(
                TestDataBuilder.createOrderResponseDto(1L),
                TestDataBuilder.createOrderResponseDto(2L)
        );
    }

    // ==================== TESTS POST - CREATE ORDER ====================

    @Test
    @DisplayName("POST /api/v1/orders - Devrait créer une commande avec succès")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithValidData_ShouldReturnSuccessMessage() throws Exception {
        // Given
        doNothing().when(iOrderService).createOrder(any(OrderRequestDto.class));
        String requestJson = objectMapper.writeValueAsString(validOrderRequest);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Order created successfully!"));

        verify(iOrderService, times(1)).createOrder(any(OrderRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait échouer sans authentification")
    void createOrder_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Given
        String requestJson = objectMapper.writeValueAsString(validOrderRequest);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(iOrderService, never()).createOrder(any(OrderRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait gérer les erreurs du service")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WhenServiceFails_ShouldReturnServiceUnavailable() throws Exception {
        // Given
        doThrow(new RuntimeException("Database error"))
                .when(iOrderService).createOrder(any(OrderRequestDto.class));

        String requestJson = objectMapper.writeValueAsString(validOrderRequest);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.errorMessage", containsString("Une erreur technique")))
                .andExpect(jsonPath("$.errorTime").exists());

        verify(iOrderService, times(1)).createOrder(any(OrderRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait échouer avec body vide")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithInvalidBody_ShouldReturnBadRequest() throws Exception {
        // Given
        String emptyJson = "{}";

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists())
                .andExpect(jsonPath("$.errors.totalPrice").value("Le prix total est obligatoire"))
                .andExpect(jsonPath("$.errors.paymentId").value("L'ID de paiement est obligatoire"))
                .andExpect(jsonPath("$.errors.items").value("La commande doit contenir au moins un article"))
                .andExpect(jsonPath("$.errors.paymentStatus").value("Le statut de paiement est obligatoire"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verifyNoInteractions(iOrderService);
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait échouer avec Content-Type invalide sans appeler le service")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithInvalidContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // Given
        String validJson = "{\"totalPrice\":175.00,\"paymentId\":\"pi_test\",\"paymentStatus\":\"paid\",\"items\":[]}";

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(validJson))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());

        // le service n'est jamais appeler
        verify(iOrderService, never()).createOrder(any(OrderRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait appeler le service avec les bons paramètres")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_ShouldCallServiceWithCorrectParameters() throws Exception {
        // Given
        doNothing().when(iOrderService).createOrder(any(OrderRequestDto.class));
        String requestJson = objectMapper.writeValueAsString(validOrderRequest);

        // When
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - Vérifier que le service a été appelé avec les bons paramètres
        verify(iOrderService).createOrder(argThat(dto ->
                dto.totalPrice().compareTo(new BigDecimal("175.00")) == 0 &&
                        dto.paymentId().equals("pi_test_123456") &&
                        dto.paymentStatus().equals("paid") &&
                        dto.items().size() == 2
        ));
    }

    // ==================== TESTS GET - LOAD CUSTOMER ORDERS ====================

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner toutes les commandes du client")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnOrderList() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].orderId", is(1)))
                .andExpect(jsonPath("$[0].status", is(ApplicationConstants.ORDER_STATUS_CREATED)))
                .andExpect(jsonPath("$[0].totalPrice", is(175.00)))
                .andExpect(jsonPath("$[1].orderId", is(2)));

        verify(iOrderService, times(1)).getCustomerOrders();
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner une liste vide si aucune commande")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_WhenNoOrders_ShouldReturnEmptyList() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(iOrderService, times(1)).getCustomerOrders();
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait échouer sans authentification")
    void loadCustomerOrders_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(iOrderService, never()).getCustomerOrders();
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait gérer les erreurs du service")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_WhenServiceFails_ShouldReturnServiceUnavailable() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders())
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.errorMessage", containsString("Une erreur technique")));

        verify(iOrderService, times(1)).getCustomerOrders();
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner les bons Content-Type headers")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnCorrectContentTypeHeaders() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ==================== TESTS DE VALIDATION DES DONNÉES ====================

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner tous les champs requis")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnAllRequiredFields() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").exists())
                .andExpect(jsonPath("$[0].status").exists())
                .andExpect(jsonPath("$[0].totalPrice").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].items").exists())
                .andExpect(jsonPath("$[0].items").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner les prix avec 2 décimales")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnPricesWithTwoDecimals() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalPrice", is(175.00)))
                .andExpect(jsonPath("$[1].totalPrice", is(175.00)));
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner les items avec leurs détails")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnItemsWithDetails() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].items", hasSize(2)))
                .andExpect(jsonPath("$[0].items[0].productName").exists())
                .andExpect(jsonPath("$[0].items[0].quantity").exists())
                .andExpect(jsonPath("$[0].items[0].price").exists())
                .andExpect(jsonPath("$[0].items[0].imageUrl").exists());
    }

    // ==================== TESTS DE PERFORMANCE ====================

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner rapidement même avec beaucoup de commandes")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_WithManyOrders_ShouldReturnQuickly() throws Exception {
        // Given - Créer 50 commandes
        List<OrderResponseDto> manyOrders = java.util.stream.IntStream.range(1, 51)
                .mapToObj(i -> TestDataBuilder.createOrderResponseDto((long) i))
                .toList();

        when(iOrderService.getCustomerOrders()).thenReturn(manyOrders);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)))
                .andExpect(jsonPath("$[0].orderId", is(1)))
                .andExpect(jsonPath("$[49].orderId", is(50)));

        verify(iOrderService, times(1)).getCustomerOrders();
    }

    // ==================== TESTS DE CACHE ====================

    @Test
    @DisplayName("Devrait appeler le service à chaque fois (pas de cache dans les tests unitaires)")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldCallServiceEveryTime() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When - Appeler 3 fois
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isOk());

        // Then - Le service devrait être appelé 3 fois
        verify(iOrderService, times(3)).getCustomerOrders();
    }

    // ==================== TESTS MÉTIER ====================

    @Test
    @DisplayName("Devrait retourner les commandes dans l'ordre retourné par le service")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnOrdersInServiceOrder() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId", is(1)))
                .andExpect(jsonPath("$[1].orderId", is(2)));
    }

    @Test
    @DisplayName("Devrait retourner exactement ce que le service retourne")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnExactlyWhatServiceReturns() throws Exception {
        // Given
        OrderResponseDto singleOrder = TestDataBuilder.createOrderResponseDto(999L);
        when(iOrderService.getCustomerOrders()).thenReturn(List.of(singleOrder));

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId", is(999)))
                .andExpect(jsonPath("$[0].status", is(ApplicationConstants.ORDER_STATUS_CREATED)))
                .andExpect(jsonPath("$[0].totalPrice", is(175.00)));
    }

    // ==================== TESTS D'INTÉGRATION DES ENDPOINTS ====================

    @Test
    @DisplayName("POST puis GET - Devrait créer une commande puis la récupérer")
    @WithMockUser(username = "john.doe@example.com")
    void createOrderThenGet_ShouldWorkCorrectly() throws Exception {
        // Given
        doNothing().when(iOrderService).createOrder(any(OrderRequestDto.class));
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        String requestJson = objectMapper.writeValueAsString(validOrderRequest);

        // When & Then - Créer
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // When & Then - Récupérer
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(iOrderService, times(1)).createOrder(any(OrderRequestDto.class));
        verify(iOrderService, times(1)).getCustomerOrders();
    }

    // ==================== TESTS DE VALIDATION JSON ====================

    @Test
    @DisplayName("POST /api/v1/orders - Devrait accepter un JSON valide avec tous les champs")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithAllFields_ShouldAcceptValidJson() throws Exception {
        // Given
        OrderRequestDto completeOrder = new OrderRequestDto(
                new BigDecimal("999.99"),
                "pi_complete_test_123456789",
                "succeeded",
                List.of(
                        new OrderItemDto(1L, 3, new BigDecimal("333.33")),
                        new OrderItemDto(2L, 2, new BigDecimal("166.66"))
                )
        );

        doNothing().when(iOrderService).createOrder(any(OrderRequestDto.class));
        String requestJson = objectMapper.writeValueAsString(completeOrder);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Order created successfully!"));

        verify(iOrderService, times(1)).createOrder(any(OrderRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait échouer avec JSON malformé sans appeler le service")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        // Given
        String malformedJson = "{invalid json}";

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // ✅ Vérification PROFESSIONNELLE
        verify(iOrderService, never()).createOrder(any(OrderRequestDto.class));
    }

    // ==================== TESTS DE SÉCURITÉ ====================

    @Test
    @DisplayName("Devrait vérifier que seul l'utilisateur authentifié peut créer des commandes")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_ShouldOnlyAllowAuthenticatedUser() throws Exception {
        // Given
        doNothing().when(iOrderService).createOrder(any(OrderRequestDto.class));
        String requestJson = objectMapper.writeValueAsString(validOrderRequest);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Vérifier qu'un utilisateur authentifié peut créer
        verify(iOrderService, times(1)).createOrder(any(OrderRequestDto.class));
    }

    @Test
    @DisplayName("Devrait vérifier que seul l'utilisateur authentifié peut voir ses commandes")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldOnlyAllowAuthenticatedUser() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk());

        // Vérifier qu'un utilisateur authentifié peut récupérer ses commandes
        verify(iOrderService, times(1)).getCustomerOrders();
    }

    // ==================== TESTS D'ERREURS SPÉCIFIQUES ====================

    @Test
    @DisplayName("POST /api/v1/orders - Devrait retourner 404 si produit non trouvé")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WhenProductNotFound_ShouldReturnNotFound() throws Exception {
        // Given
        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("175.00"),
                "pi_test_123456",
                "paid",
                List.of(new OrderItemDto(999L, 1, new BigDecimal("175.00"))) // ID produit inexistant
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // Simuler ResourceNotFoundException (comme le fait votre service)
        doThrow(new ResourceNotFoundException("Product", "ProductID", "999"))
                .when(iOrderService).createOrder(any(OrderRequestDto.class));

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isNotFound()) // Doit retourner 404
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("La ressource demandée n'a pas été trouvée."));

        log.info("✅ Gestion correcte du produit non trouvé");
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait gérer les erreurs de paiement")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WhenPaymentFails_ShouldReturnError() throws Exception {
        // Given
        doThrow(new RuntimeException("Payment failed"))
                .when(iOrderService).createOrder(any(OrderRequestDto.class));

        String requestJson = objectMapper.writeValueAsString(validOrderRequest);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorMessage", containsString("Une erreur technique")));

        verify(iOrderService, times(1)).createOrder(any(OrderRequestDto.class));
    }

    // ==================== TESTS DE TYPES DE DONNÉES ====================

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner les bons types de données")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnCorrectDataTypes() throws Exception {
        // Given
        when(iOrderService.getCustomerOrders()).thenReturn(testOrderResponses);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").isNumber())
                .andExpect(jsonPath("$[0].status").isString())  // Changé de orderStatus à status
                .andExpect(jsonPath("$[0].totalPrice").isNumber())
                .andExpect(jsonPath("$[0].createdAt").isString())
                .andExpect(jsonPath("$[0].items").isArray());
    }
}