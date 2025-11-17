package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.order.OrderItemDto;
import com.store.store.dto.order.OrderRequestDto;
import com.store.store.entity.Category;
import com.store.store.entity.Order;
import com.store.store.entity.Product;
import com.store.store.repository.CategoryRepository;
import com.store.store.repository.OrderRepository;
import com.store.store.repository.ProductRepository;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'INTÉGRATION - Base de données réelle (H2)
 * Objectif : Tester les scénarios business de bout en bout
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Tests d'Intégration - OrderController")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;  // ✅ AJOUTÉ

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Category testCategory;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        // ✅ CORRIGER : Créer d'abord la catégorie
        testCategory = TestDataBuilder.createDefaultCategory();
        testCategory = categoryRepository.save(testCategory);

        // ✅ CORRIGER : Créer le produit avec la catégorie
        testProduct = TestDataBuilder.createProduct(
                null,  // ID auto-généré
                "Test Product",
                new BigDecimal("49.99"),
                testCategory
        );
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        // Nettoyage optionnel (si @Transactional ne suffit pas)
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    // =====================================================
    // TESTS DE SUCCÈS
    // =====================================================

    @Test
    @DisplayName("Scénario complet : Créer et récupérer une commande")
    @WithMockUser(username = "customer@test.com")
    void completeOrderFlow_Success() throws Exception {
        // Given
        OrderRequestDto request = TestDataBuilder.createOrderRequest(
                testProduct.getId(),
                2,
                testProduct.getPrice()
        );

        // When - Créer la commande
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Then - Vérifier en base
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getTotalPrice())
                .isEqualByComparingTo(testProduct.getPrice().multiply(BigDecimal.valueOf(2)));
        assertThat(orders.get(0).getOrderItems()).hasSize(1);

        // And - Récupérer via API
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderStatus").exists());
    }

    @Test
    @DisplayName("Créer une commande avec plusieurs produits")
    @WithMockUser(username = "customer@test.com")
    void createOrder_MultipleProducts_Success() throws Exception {
        // Given - Créer un deuxième produit
        Product product2 = TestDataBuilder.createProduct(
                null,
                "Product 2",
                new BigDecimal("29.99"),
                testCategory
        );
        product2 = productRepository.save(product2);

        OrderRequestDto request = new OrderRequestDto(
                new BigDecimal("179.97"),  // (49.99 * 2) + (29.99 * 3)
                "pi_test_multi",
                "paid",
                List.of(
                        new OrderItemDto(testProduct.getId(), 2, testProduct.getPrice()),
                        new OrderItemDto(product2.getId(), 3, product2.getPrice())
                )
        );

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify
        List<Order> orders = orderRepository.findAll();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getOrderItems()).hasSize(2);
    }

    // =====================================================
    // TESTS D'ERREUR
    // =====================================================

    @Test
    @DisplayName("Échec : Produit inexistant")
    @WithMockUser
    void createOrder_ProductNotFound_ReturnsNotFound() throws Exception {
        // Given
        OrderRequestDto request = TestDataBuilder.createOrderRequest(
                9999L,  // ID inexistant
                1,
                new BigDecimal("99.99")
        );

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists());

        // Verify - Aucune commande créée
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Échec : Quantité invalide (négative)")
    @WithMockUser
    void createOrder_NegativeQuantity_ReturnsBadRequest() throws Exception {
        // Given
        OrderRequestDto request = new OrderRequestDto(
                new BigDecimal("99.99"),
                "pi_test_negative",
                "paid",
                List.of(new OrderItemDto(testProduct.getId(), -1, testProduct.getPrice()))
        );

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());

        // Verify
        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Échec : Quantité zéro")
    @WithMockUser
    void createOrder_ZeroQuantity_ReturnsBadRequest() throws Exception {
        // Given
        OrderRequestDto request = new OrderRequestDto(
                new BigDecimal("0.00"),
                "pi_test_zero",
                "paid",
                List.of(new OrderItemDto(testProduct.getId(), 0, testProduct.getPrice()))
        );

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Échec : Prix total négatif")
    @WithMockUser
    void createOrder_NegativeTotalPrice_ReturnsBadRequest() throws Exception {
        // Given
        OrderRequestDto request = new OrderRequestDto(
                new BigDecimal("-99.99"),
                "pi_test_negative_price",
                "paid",
                List.of(new OrderItemDto(testProduct.getId(), 1, testProduct.getPrice()))
        );

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Échec : Liste d'items vide")
    @WithMockUser
    void createOrder_EmptyItems_ReturnsBadRequest() throws Exception {
        // Given
        OrderRequestDto request = new OrderRequestDto(
                new BigDecimal("99.99"),
                "pi_test_empty",
                "paid",
                List.of()  // Liste vide
        );

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("Échec : Champs obligatoires manquants")
    @WithMockUser
    void createOrder_MissingFields_ReturnsBadRequest() throws Exception {
        // Given
        String invalidJson = """
            {
                "totalPrice": null,
                "paymentIntentId": null,
                "paymentStatus": null,
                "items": []
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    // =====================================================
    // TESTS DE SÉCURITÉ
    // =====================================================

    @Test
    @DisplayName("Échec : Utilisateur non authentifié")
    void createOrder_Unauthenticated_ReturnsUnauthorized() throws Exception {
        // Given
        OrderRequestDto request = TestDataBuilder.createValidOrderRequest();

        // When & Then - Pas de @WithMockUser
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Récupérer les commandes d'un client")
    @WithMockUser(username = "customer@test.com")
    void getCustomerOrders_Success() throws Exception {
        // Given - Créer une commande d'abord
        OrderRequestDto request = TestDataBuilder.createOrderRequest(
                testProduct.getId(),
                1,
                testProduct.getPrice()
        );

        mockMvc.perform(post("/api/v1/orders")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // =====================================================
    // TESTS DE PERFORMANCE
    // =====================================================

    @Test
    @DisplayName("Performance : Créer 10 commandes")
    @WithMockUser(username = "customer@test.com")
    void createOrder_Performance_MultipleOrders() throws Exception {
        // Given
        int orderCount = 10;

        // When
        for (int i = 0; i < orderCount; i++) {
            OrderRequestDto request = TestDataBuilder.createOrderRequest(
                    testProduct.getId(),
                    1,
                    testProduct.getPrice()
            );

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // Then
        assertThat(orderRepository.findAll()).hasSize(orderCount);
    }
}