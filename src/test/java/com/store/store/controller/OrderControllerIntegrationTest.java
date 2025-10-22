package com.store.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.OrderItemDto;
import com.store.store.dto.OrderRequestDto;
import com.store.store.entity.*;
import com.store.store.repository.*;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("Tests d'Intégration - OrderController")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    private Customer testCustomer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        // Nettoyer les données
        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();

        // Créer un client de test
        testCustomer = TestDataBuilder.createCustomer(null, "John", "Doe", "john.doe@example.com");
        testCustomer = customerRepository.save(testCustomer);

        // Créer des produits de test
        product1 = TestDataBuilder.createProduct(null, "Laptop", new BigDecimal("999.99"));
        product2 = TestDataBuilder.createProduct(null, "Mouse", new BigDecimal("29.99"));

        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);

        log.info("✅ Configuration test terminée - Customer: {}, Products: {}",
                testCustomer.getEmail(), List.of(product1.getName(), product2.getName()));
    }

    // ==================== TESTS POST - CREATE ORDER ====================

    @Test
    @DisplayName("POST /api/v1/orders - Devrait créer une commande avec succès")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithValidData_ShouldCreateSuccessfully() throws Exception {
        // Given
        List<OrderItemDto> items = List.of(
                new OrderItemDto(product1.getId(), 2, new BigDecimal("999.99")),
                new OrderItemDto(product2.getId(), 1, new BigDecimal("29.99"))
        );

        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("2029.97"),
                "pi_test_123456789",
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf()) // ✅ Ajout du token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Order created successfully!"));

        // Vérifier que la commande a été créée en base
        List<Order> orders = orderRepository.findAll();
        assert orders.size() == 1;
        assert orders.get(0).getTotalPrice().compareTo(new BigDecimal("2029.97")) == 0;
        assert orders.get(0).getOrderItems().size() == 2;

        log.info("✅ Commande créée avec succès - ID: {}", orders.get(0).getOrderId());
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait échouer avec des données invalides")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given - Commande sans items
        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("0.00"),
                "pi_test_invalid",
                "paid",
                List.of()
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        log.info("✅ Validation correcte des données invalides");
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait échouer si produit inexistant")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithNonExistentProduct_ShouldReturnError() throws Exception {
        // Given - Produit qui n'existe pas
        List<OrderItemDto> items = List.of(
                new OrderItemDto(99999L, 1, new BigDecimal("100.00"))
        );

        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("100.00"),
                "pi_test_123456",
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isNotFound());

        log.info("✅ Gestion correcte du produit inexistant");
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait échouer sans authentification")
    void createOrder_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Given
        List<OrderItemDto> items = List.of(
                new OrderItemDto(product1.getId(), 1, new BigDecimal("999.99"))
        );

        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("999.99"),
                "pi_test_123456",
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("✅ Sécurité: authentification requise vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait créer une commande avec plusieurs items")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithMultipleItems_ShouldCreateSuccessfully() throws Exception {
        // Given
        Product product3 = TestDataBuilder.createProduct(null, "Keyboard", new BigDecimal("79.99"));
        product3 = productRepository.save(product3);

        List<OrderItemDto> items = List.of(
                new OrderItemDto(product1.getId(), 2, new BigDecimal("999.99")),
                new OrderItemDto(product2.getId(), 3, new BigDecimal("29.99")),
                new OrderItemDto(product3.getId(), 1, new BigDecimal("79.99"))
        );

        BigDecimal totalPrice = new BigDecimal("999.99")
                .multiply(new BigDecimal("2"))
                .add(new BigDecimal("29.99").multiply(new BigDecimal("3")))
                .add(new BigDecimal("79.99"));

        OrderRequestDto requestDto = new OrderRequestDto(
                totalPrice,
                "pi_test_multiple_items",
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Order created successfully!"));

        // Vérifier en base
        List<Order> orders = orderRepository.findAll();
        assert orders.size() == 1;
        assert orders.get(0).getOrderItems().size() == 3;

        log.info("✅ Commande avec 3 items créée avec succès");
    }

    // ==================== TESTS GET - LOAD CUSTOMER ORDERS ====================

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner toutes les commandes du client")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnAllOrders() throws Exception {
        // Given - Créer 2 commandes pour le client
        Order order1 = TestDataBuilder.createOrder(null, testCustomer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, testCustomer, ApplicationConstants.ORDER_STATUS_CREATED);

        orderRepository.saveAll(List.of(order1, order2));

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status", is(ApplicationConstants.ORDER_STATUS_CREATED)))  // ✅ Changé de orderStatus à status
                .andExpect(jsonPath("$[1].status", is(ApplicationConstants.ORDER_STATUS_CREATED)));

        log.info("✅ Commandes du client récupérées avec succès");
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner une liste vide si aucune commande")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_WhenNoOrders_ShouldReturnEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        log.info("✅ Liste vide retournée correctement");
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait échouer sans authentification")
    void loadCustomerOrders_WithoutAuthentication_ShouldReturnUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        log.info("✅ Sécurité: authentification requise vérifiée");
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner uniquement les commandes du client connecté")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnOnlyCurrentCustomerOrders() throws Exception {
        // Given - Créer un autre client avec une commande
        Customer otherCustomer = TestDataBuilder.createCustomer(null, "Jane", "Smith", "jane.smith@example.com");
        otherCustomer = customerRepository.save(otherCustomer);

        Order order1 = TestDataBuilder.createOrder(null, testCustomer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, otherCustomer, ApplicationConstants.ORDER_STATUS_CREATED);

        orderRepository.saveAll(List.of(order1, order2));

        // When & Then - Seule la commande de testCustomer doit être retournée
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        log.info("✅ Isolation des commandes par client vérifiée");
    }

    @Test
    @DisplayName("GET /api/v1/orders - Devrait retourner les commandes avec leurs items")
    @WithMockUser(username = "john.doe@example.com")
    void loadCustomerOrders_ShouldReturnOrdersWithItems() throws Exception {
        // Given - Créer une commande avec des produits persistés
        Order order = TestDataBuilder.createOrder(null, testCustomer, ApplicationConstants.ORDER_STATUS_CREATED);

        OrderItem item1 = new OrderItem();
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setPrice(product1.getPrice());
        item1.setOrder(order);

        OrderItem item2 = new OrderItem();
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setPrice(product2.getPrice());
        item2.setOrder(order);

        order.setOrderItems(List.of(item1, item2));
        orderRepository.save(order);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId").exists())
                .andExpect(jsonPath("$[0].status").exists())  // ✅ Changé de orderStatus à status
                .andExpect(jsonPath("$[0].totalPrice").exists())
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[0].items").isArray())
                .andExpect(jsonPath("$[0].items", hasSize(2)));

        log.info("✅ Commandes avec items récupérées correctement");
    }

    // ==================== TESTS DE VALIDATION ====================

    @Test
    @DisplayName("POST /api/v1/orders - Devrait valider le format du paymentId")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithInvalidPaymentId_ShouldReturnBadRequest() throws Exception {
        // Given - PaymentId vide
        List<OrderItemDto> items = List.of(
                new OrderItemDto(product1.getId(), 1, new BigDecimal("999.99"))
        );

        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("999.99"),
                "",  // PaymentId vide
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        log.info("✅ Validation du paymentId vérifiée");
    }

    @Test
    @DisplayName("POST /api/v1/orders - Devrait valider que le prix total est positif")
    @WithMockUser(username = "john.doe@example.com")
    void createOrder_WithNegativeTotalPrice_ShouldReturnBadRequest() throws Exception {
        // Given - Prix négatif
        List<OrderItemDto> items = List.of(
                new OrderItemDto(product1.getId(), 1, new BigDecimal("999.99"))
        );

        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("-100.00"),  // Prix négatif
                "pi_test_123456",
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When & Then
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andDo(print())
                .andExpect(status().isBadRequest());

        log.info("✅ Validation du prix total vérifiée");
    }

    // ==================== TESTS DE PERSISTANCE ====================

    @Test
    @DisplayName("Devrait vérifier que les commandes sont bien persistées en base")
    @WithMockUser(username = "john.doe@example.com")
    void shouldVerifyOrdersPersistence() throws Exception {
        // Given
        List<OrderItemDto> items = List.of(
                new OrderItemDto(product1.getId(), 1, new BigDecimal("999.99"))
        );

        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("999.99"),
                "pi_persistence_test",
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then - Vérifier en base
        List<Order> ordersInDb = orderRepository.findAll();
        assert ordersInDb.size() == 1;
        assert ordersInDb.get(0).getPaymentId().equals("pi_persistence_test");
        assert ordersInDb.get(0).getCustomer().getEmail().equals("john.doe@example.com");

        log.info("✅ Persistance des commandes vérifiée avec succès");
    }

    @Test
    @DisplayName("Devrait vérifier que les OrderItems sont bien liés à la commande")
    @WithMockUser(username = "john.doe@example.com")
    void shouldVerifyOrderItemsAreLinkedToOrder() throws Exception {
        // Given
        List<OrderItemDto> items = List.of(
                new OrderItemDto(product1.getId(), 2, new BigDecimal("999.99")),
                new OrderItemDto(product2.getId(), 1, new BigDecimal("29.99"))
        );

        OrderRequestDto requestDto = new OrderRequestDto(
                new BigDecimal("2029.97"),
                "pi_items_test",
                "paid",
                items
        );

        String requestJson = objectMapper.writeValueAsString(requestDto);

        // When
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk());

        // Then
        List<Order> ordersInDb = orderRepository.findAll();
        Order savedOrder = ordersInDb.get(0);

        assert savedOrder.getOrderItems().size() == 2;
        assert savedOrder.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(product1.getId()));
        assert savedOrder.getOrderItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(product2.getId()));

        log.info("✅ Liaison OrderItems vérifiée avec succès");
    }

    // ==================== TESTS DE TRANSACTION ====================

    @Test
    @DisplayName("Devrait rollback les changements après le test grâce à @Transactional")
    @WithMockUser(username = "john.doe@example.com")
    void shouldRollbackChangesAfterTest() throws Exception {
        long initialCount = orderRepository.count();

        Order tempOrder = TestDataBuilder.createOrder(null, testCustomer, ApplicationConstants.ORDER_STATUS_CREATED);
        orderRepository.save(tempOrder);

        assert orderRepository.count() == initialCount + 1;

        log.info("✅ Transaction test : {} commandes avant, {} après ajout",
                initialCount, orderRepository.count());
    }

    // ==================== TESTS DE VALIDATION DES DONNÉES ====================

    @Test
    @DisplayName("Devrait retourner les bons types de données pour GET /orders")
    @WithMockUser(username = "john.doe@example.com")
    void shouldReturnCorrectDataTypesForOrders() throws Exception {
        // Given - Créer une commande avec des produits persistés
        Order order = TestDataBuilder.createOrder(null, testCustomer, ApplicationConstants.ORDER_STATUS_CREATED);

        OrderItem item = new OrderItem();
        item.setProduct(product1);
        item.setQuantity(1);
        item.setPrice(product1.getPrice());
        item.setOrder(order);

        order.setOrderItems(List.of(item));
        orderRepository.save(order);

        // When & Then
        mockMvc.perform(get("/api/v1/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").isNumber())
                .andExpect(jsonPath("$[0].status").isString())  // ✅ Changé de orderStatus à status
                .andExpect(jsonPath("$[0].totalPrice").isNumber())
                .andExpect(jsonPath("$[0].createdAt").isString())
                .andExpect(jsonPath("$[0].items").isArray());

        log.info("✅ Types de données vérifiés avec succès");
    }
}