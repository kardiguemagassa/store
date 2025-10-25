package com.store.store.repository;

import com.store.store.constants.ApplicationConstants;
import com.store.store.entity.Customer;
import com.store.store.entity.Order;
import com.store.store.entity.OrderItem;
import com.store.store.entity.Product;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour OrderRepository
 * Teste les opérations CRUD et les requêtes personnalisées avec relations
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du OrderRepository")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    // ==================== TESTS CRUD DE BASE ====================

    @Test
    @DisplayName("Devrait sauvegarder une commande avec succès")
    void shouldSaveOrder() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "John", "Doe", "john@example.com");
        entityManager.persist(customer);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);

        // When
        Order savedOrder = orderRepository.save(order);

        // Then
        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getOrderId()).isNotNull();
        assertThat(savedOrder.getCustomer().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(savedOrder.getOrderStatus()).isEqualTo(ApplicationConstants.ORDER_STATUS_CREATED);
        assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(savedOrder.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Devrait sauvegarder une commande avec des items")
    void shouldSaveOrderWithItems() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Jane", "Smith", "jane@example.com");
        entityManager.persist(customer);

        Product product1 = TestDataBuilder.createProduct(null, "Product 1", new BigDecimal("50.00"));
        Product product2 = TestDataBuilder.createProduct(null, "Product 2", new BigDecimal("75.00"));
        entityManager.persist(product1);
        entityManager.persist(product2);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        entityManager.persist(order);

        OrderItem item1 = TestDataBuilder.createOrderItem(null, order, product1, 2, new BigDecimal("50.00"));
        OrderItem item2 = TestDataBuilder.createOrderItem(null, order, product2, 1, new BigDecimal("75.00"));
        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);
        order.setTotalPrice(new BigDecimal("175.00"));

        // When
        Order savedOrder = orderRepository.save(order);
        entityManager.flush();

        // Then
        assertThat(savedOrder.getOrderItems()).hasSize(2);
        assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo(new BigDecimal("175.00"));
    }

    @Test
    @DisplayName("Devrait trouver une commande par son ID")
    void shouldFindOrderById() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Test", "User", "test@example.com");
        entityManager.persist(customer);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order savedOrder = entityManager.persistAndFlush(order);
        Long orderId = savedOrder.getOrderId();

        // When
        Optional<Order> foundOrder = orderRepository.findById(orderId);

        // Then
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getCustomer().getEmail()).isEqualTo("test@example.com"); // ✅ Correct
        // OU si vous voulez vérifier le nom :
        assertThat(foundOrder.get().getCustomer().getName()).isEqualTo("Test User"); // ✅ Prénom + Nom
        assertThat(foundOrder.get().getOrderStatus()).isEqualTo(ApplicationConstants.ORDER_STATUS_CREATED);
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty() pour un ID inexistant")
    void shouldReturnEmptyForNonExistentId() {
        // When
        Optional<Order> foundOrder = orderRepository.findById(999L);

        // Then
        assertThat(foundOrder).isEmpty();
    }

    @Test
    @DisplayName("Devrait récupérer toutes les commandes")
    void shouldFindAllOrders() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "Customer", "One", "c1@example.com");
        Customer customer2 = TestDataBuilder.createCustomer(null, "Customer", "Two", "c2@example.com");
        entityManager.persist(customer1);
        entityManager.persist(customer2);

        Order order1 = TestDataBuilder.createOrder(null, customer1, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer2, ApplicationConstants.ORDER_STATUS_SHIPPED);
        Order order3 = TestDataBuilder.createOrder(null, customer1, ApplicationConstants.ORDER_STATUS_DELIVERED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // When
        List<Order> foundOrders = orderRepository.findAll();

        // Then
        assertThat(foundOrders).hasSize(3);
    }

    @Test
    @DisplayName("Devrait mettre à jour une commande existante")
    void shouldUpdateExistingOrder() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Update", "Test", "update@example.com");
        entityManager.persist(customer);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order savedOrder = entityManager.persistAndFlush(order);
        Long orderId = savedOrder.getOrderId();

        // When
        savedOrder.setOrderStatus(ApplicationConstants.ORDER_STATUS_SHIPPED);
        orderRepository.save(savedOrder);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> updatedOrder = orderRepository.findById(orderId);
        assertThat(updatedOrder).isPresent();
        assertThat(updatedOrder.get().getOrderStatus()).isEqualTo(ApplicationConstants.ORDER_STATUS_SHIPPED);
    }

    @Test
    @DisplayName("Devrait supprimer une commande par son ID")
    void shouldDeleteOrderById() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Delete", "Test", "delete@example.com");
        entityManager.persist(customer);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order savedOrder = entityManager.persistAndFlush(order);
        Long orderId = savedOrder.getOrderId();

        // When
        orderRepository.deleteById(orderId);
        entityManager.flush();

        // Then
        Optional<Order> deletedOrder = orderRepository.findById(orderId);
        assertThat(deletedOrder).isEmpty();
    }

    @Test
    @DisplayName("Devrait compter le nombre total de commandes")
    void shouldCountAllOrders() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Count", "Test", "count@example.com");
        entityManager.persist(customer);

        Order order1 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_SHIPPED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();

        // When
        long count = orderRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== TESTS REQUÊTES PAR CUSTOMER ====================

    @Test
    @DisplayName("findByCustomerOrderByCreatedAtDesc - Devrait trouver les commandes d'un client triées")
    void shouldFindOrdersByCustomerSortedByDate() throws InterruptedException {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "Customer", "One", "c1@example.com");
        Customer customer2 = TestDataBuilder.createCustomer(null, "Customer", "Two", "c2@example.com");
        entityManager.persist(customer1);
        entityManager.persist(customer2);

        Order order1 = TestDataBuilder.createOrder(null, customer1, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer1, ApplicationConstants.ORDER_STATUS_SHIPPED);
        Order order3 = TestDataBuilder.createOrder(null, customer2, ApplicationConstants.ORDER_STATUS_CREATED);

        entityManager.persist(order1);
        Thread.sleep(10); // Petit délai pour différencier les dates
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // When
        List<Order> customer1Orders = orderRepository.findByCustomerOrderByCreatedAtDesc(customer1);

        // Then
        assertThat(customer1Orders).hasSize(2);
        assertThat(customer1Orders.get(0).getCreatedAt())
                .isAfterOrEqualTo(customer1Orders.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("findOrdersByCustomer (JPQL) - Devrait trouver les commandes d'un client")
    void shouldFindOrdersByCustomerUsingJPQL() {
        // Given
        Customer customer1 = TestDataBuilder.createCustomer(null, "JPQL", "Test", "jpql@example.com");
        Customer customer2 = TestDataBuilder.createCustomer(null, "Other", "Customer", "other@example.com");
        entityManager.persist(customer1);
        entityManager.persist(customer2);

        Order order1 = TestDataBuilder.createOrder(null, customer1, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer1, ApplicationConstants.ORDER_STATUS_SHIPPED);
        Order order3 = TestDataBuilder.createOrder(null, customer2, ApplicationConstants.ORDER_STATUS_CREATED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // When
        List<Order> customer1Orders = orderRepository.findOrdersByCustomer(customer1);

        // Then
        assertThat(customer1Orders).hasSize(2);
        assertThat(customer1Orders).allMatch(o -> o.getCustomer().equals(customer1));
    }

    @Test
    @DisplayName("findOrdersByCustomerWithNativeQuery - Devrait utiliser la requête native")
    void shouldFindOrdersByCustomerUsingNativeQuery() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Native", "Test", "native@example.com");
        entityManager.persist(customer);

        Order order1 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_SHIPPED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();

        // When
        List<Order> orders = orderRepository.findOrdersByCustomerWithNativeQuery(customer.getCustomerId());

        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomer().getCustomerId().equals(customer.getCustomerId()));
    }

    // ==================== TESTS REQUÊTES PAR STATUS ====================

    @Test
    @DisplayName("findByOrderStatus - Devrait trouver les commandes par statut CREATED")
    void shouldFindOrdersByStatusCreated() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Status", "Test", "status@example.com");
        entityManager.persist(customer);

        Order order1 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order3 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_SHIPPED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // When
        List<Order> createdOrders = orderRepository.findByOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);

        // Then
        assertThat(createdOrders).hasSize(2);
        assertThat(createdOrders).allMatch(o ->
                o.getOrderStatus().equals(ApplicationConstants.ORDER_STATUS_CREATED));
    }

    @Test
    @DisplayName("findOrdersByStatus (JPQL) - Devrait trouver les commandes par statut")
    void shouldFindOrdersByStatusUsingJPQL() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "JPQL", "Status", "jpqlstatus@example.com");
        entityManager.persist(customer);

        Order order1 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_SHIPPED);
        Order order2 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_SHIPPED);
        Order order3 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_DELIVERED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // When
        List<Order> shippedOrders = orderRepository.findOrdersByStatus(ApplicationConstants.ORDER_STATUS_SHIPPED);

        // Then
        assertThat(shippedOrders).hasSize(2);
        assertThat(shippedOrders).allMatch(o ->
                o.getOrderStatus().equals(ApplicationConstants.ORDER_STATUS_SHIPPED));
    }

    @Test
    @DisplayName("findOrdersByStatusWithNativeQuery - Devrait utiliser la requête native")
    void shouldFindOrdersByStatusUsingNativeQuery() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Native", "Status", "nativestatus@example.com");
        entityManager.persist(customer);

        Order order1 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_DELIVERED);
        Order order2 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_DELIVERED);
        Order order3 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // When
        List<Order> deliveredOrders = orderRepository.findOrdersByStatusWithNativeQuery(
                ApplicationConstants.ORDER_STATUS_DELIVERED);

        // Then
        assertThat(deliveredOrders).hasSize(2);
        assertThat(deliveredOrders).allMatch(o ->
                o.getOrderStatus().equals(ApplicationConstants.ORDER_STATUS_DELIVERED));
    }

    @Test
    @DisplayName("Devrait retourner une liste vide si aucune commande avec ce statut")
    void shouldReturnEmptyListWhenNoOrdersWithStatus() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Empty", "Status", "empty@example.com");
        entityManager.persist(customer);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        entityManager.persistAndFlush(order);

        // When
        List<Order> shippedOrders = orderRepository.findByOrderStatus(ApplicationConstants.ORDER_STATUS_SHIPPED);

        // Then
        assertThat(shippedOrders).isEmpty();
    }

    // ==================== TESTS DE PROPRIÉTÉS COMPLÈTES ====================

    @Test
    @DisplayName("Devrait persister et récupérer toutes les propriétés d'une commande")
    void shouldPersistAllOrderProperties() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Complete", "Order", "complete@example.com");
        entityManager.persist(customer);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        order.setTotalPrice(new BigDecimal("299.99"));
        //order.setPaymentId("pi_complete_test");
        order.setPaymentIntentId("pi_complete_test");
        order.setPaymentStatus("completed");

        // When
        Order savedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Order> retrievedOrder = orderRepository.findById(savedOrder.getOrderId());
        assertThat(retrievedOrder).isPresent();
        Order found = retrievedOrder.get();

        assertThat(found.getTotalPrice()).isEqualByComparingTo(new BigDecimal("299.99"));
        assertThat(found.getPaymentIntentId()).isEqualTo("pi_complete_test");
        assertThat(found.getPaymentStatus()).isEqualTo("completed");
        assertThat(found.getOrderStatus()).isEqualTo(ApplicationConstants.ORDER_STATUS_CREATED);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getCreatedBy()).isEqualTo(customer.getEmail());
    }

    @Test
    @DisplayName("Devrait gérer correctement les différents statuts de commande")
    void shouldHandleAllOrderStatuses() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "All", "Status", "allstatus@example.com");
        entityManager.persist(customer);

        Order order1 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_SHIPPED);
        Order order3 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_DELIVERED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.persist(order3);
        entityManager.flush();

        // When
        List<Order> createdOrders = orderRepository.findByOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        List<Order> shippedOrders = orderRepository.findByOrderStatus(ApplicationConstants.ORDER_STATUS_SHIPPED);
        List<Order> deliveredOrders = orderRepository.findByOrderStatus(ApplicationConstants.ORDER_STATUS_DELIVERED);

        // Then
        assertThat(createdOrders).hasSize(1);
        assertThat(shippedOrders).hasSize(1);
        assertThat(deliveredOrders).hasSize(1);
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'une commande par ID")
    void shouldCheckOrderExistence() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Exists", "Test", "exists@example.com");
        entityManager.persist(customer);

        Order order = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order savedOrder = entityManager.persistAndFlush(order);
        Long orderId = savedOrder.getOrderId();

        // When
        boolean exists = orderRepository.existsById(orderId);
        boolean notExists = orderRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Devrait supprimer toutes les commandes")
    void shouldDeleteAllOrders() {
        // Given
        Customer customer = TestDataBuilder.createCustomer(null, "Delete", "All", "deleteall@example.com");
        entityManager.persist(customer);

        Order order1 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_CREATED);
        Order order2 = TestDataBuilder.createOrder(null, customer, ApplicationConstants.ORDER_STATUS_SHIPPED);

        entityManager.persist(order1);
        entityManager.persist(order2);
        entityManager.flush();

        // When
        orderRepository.deleteAll();
        entityManager.flush();

        // Then
        assertThat(orderRepository.count()).isZero();
        assertThat(orderRepository.findAll()).isEmpty();
    }
}