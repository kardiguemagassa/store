/*package com.store.store.repository;

import com.store.store.constants.ApplicationConstants;
import com.store.store.entity.Customer;
import com.store.store.entity.Order;
import com.store.store.entity.OrderItem;
import com.store.store.entity.Product;
import com.store.store.repository.OrderRepository;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Tests du Repository - OrderRepository")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private Customer customer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        // Créer et persister un customer
        customer = TestDataBuilder.createCustomer();
        entityManager.persist(customer);

        // Créer et persister des produits
        product1 = TestDataBuilder.createProduct(null, "Product 1", new BigDecimal("50.00"));
        product2 = TestDataBuilder.createProduct(null, "Product 2", new BigDecimal("75.00"));
        entityManager.persist(product1);
        entityManager.persist(product2);

        entityManager.flush();
    }

    @Test
    @DisplayName("findByCustomerOrderByCreatedAtDesc - Devrait retourner les commandes du client triées par date")
    void findByCustomerOrderByCreatedAtDesc_ShouldReturnOrdersSortedByDate() {
        // Arrange
        Order order1 = createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED,
                Instant.now().minusSeconds(3600));
        Order order2 = createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CONFIRMED,
                Instant.now().minusSeconds(1800));
        Order order3 = createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED,
                Instant.now());

        // Act
        List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(customer);

        // Assert
        assertThat(orders).hasSize(3);
        assertThat(orders.get(0).getOrderId()).isEqualTo(order3.getOrderId());
        assertThat(orders.get(1).getOrderId()).isEqualTo(order2.getOrderId());
        assertThat(orders.get(2).getOrderId()).isEqualTo(order1.getOrderId());
    }

    @Test
    @DisplayName("findByCustomerOrderByCreatedAtDesc - Devrait retourner liste vide si aucune commande")
    void findByCustomerOrderByCreatedAtDesc_ShouldReturnEmptyList_WhenNoOrders() {
        // Act
        List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(customer);

        // Assert
        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("findByOrderStatus - Devrait retourner les commandes avec le statut spécifié")
    void findByOrderStatus_ShouldReturnOrdersWithGivenStatus() {
        // Arrange
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CONFIRMED, Instant.now());

        // Act
        List<Order> createdOrders = orderRepository.findByOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        List<Order> confirmedOrders = orderRepository.findByOrderStatus(ApplicationConstants.ORDER_STATUS_CONFIRMED);

        // Assert
        assertThat(createdOrders).hasSize(2);
        assertThat(confirmedOrders).hasSize(1);
        assertThat(createdOrders).allMatch(o -> o.getOrderStatus().equals(ApplicationConstants.ORDER_STATUS_CREATED));
    }

    @Test
    @DisplayName("findOrdersByCustomer (JPQL) - Devrait retourner les commandes du client")
    void findOrdersByCustomer_JPQL_ShouldReturnCustomerOrders() {
        // Arrange
        Customer anotherCustomer = TestDataBuilder.createCustomer(null, "Jane", "Doe", "jane.doe@example.com");
        entityManager.persist(anotherCustomer);

        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CONFIRMED, Instant.now());
        createAndPersistOrder(anotherCustomer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());

        // Act
        List<Order> orders = orderRepository.findOrdersByCustomer(customer);

        // Assert
        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomer().getCustomerId().equals(customer.getCustomerId()));
    }

    @Test
    @DisplayName("findOrdersByStatus (JPQL) - Devrait retourner les commandes avec le statut")
    void findOrdersByStatus_JPQL_ShouldReturnOrdersWithStatus() {
        // Arrange
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CONFIRMED, Instant.now());

        // Act
        List<Order> orders = orderRepository.findOrdersByStatus(ApplicationConstants.ORDER_STATUS_CREATED);

        // Assert
        assertThat(orders).hasSize(1);
        assertThat(orders.getFirst().getOrderStatus()).isEqualTo(ApplicationConstants.ORDER_STATUS_CREATED);
    }

    @Test
    @DisplayName("findOrdersByCustomerWithNativeQuery - Devrait retourner les commandes du client")
    void findOrdersByCustomerWithNativeQuery_ShouldReturnCustomerOrders() {
        // Arrange
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now().minusSeconds(100));
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CONFIRMED, Instant.now());

        // Act
        List<Order> orders = orderRepository.findOrdersByCustomerWithNativeQuery(customer.getCustomerId());

        // Assert
        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomer().getCustomerId().equals(customer.getCustomerId()));
        // Vérifier le tri par date DESC
        assertThat(orders.get(0).getCreatedAt()).isAfter(orders.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("findOrdersByStatusWithNativeQuery - Devrait retourner les commandes avec le statut")
    void findOrdersByStatusWithNativeQuery_ShouldReturnOrdersWithStatus() {
        // Arrange
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());
        createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CANCELLED, Instant.now());

        // Act
        List<Order> orders = orderRepository.findOrdersByStatusWithNativeQuery(ApplicationConstants.ORDER_STATUS_CREATED);

        // Assert
        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getOrderStatus().equals(ApplicationConstants.ORDER_STATUS_CREATED));
    }

    @Test
    @DisplayName("updateOrderStatus - Devrait mettre à jour le statut de la commande")
    void updateOrderStatus_ShouldUpdateOrderStatus() {
        // Arrange
        Order order = createAndPersistOrder(customer, ApplicationConstants.ORDER_STATUS_CREATED, Instant.now());
        Long orderId = order.getOrderId();
        String newStatus = ApplicationConstants.ORDER_STATUS_CONFIRMED;
        String updatedBy = "admin@example.com";

        // Act
        int updatedCount = orderRepository.updateOrderStatus(orderId, newStatus, updatedBy);
        entityManager.flush();
        entityManager.clear();

        // Assert
        assertThat(updatedCount).isEqualTo(1);

        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(newStatus);
        assertThat(updatedOrder.getUpdatedBy()).isEqualTo(updatedBy);
        assertThat(updatedOrder.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateOrderStatus - Devrait retourner 0 si la commande n'existe pas")
    void updateOrderStatus_ShouldReturn0_WhenOrderNotFound() {
        // Act
        int updatedCount = orderRepository.updateOrderStatus(999L,
                ApplicationConstants.ORDER_STATUS_CONFIRMED, "admin@example.com");

        // Assert
        assertThat(updatedCount).isZero();
    }

    @Test
    @DisplayName("save - Devrait persister une commande avec ses items")
    void save_ShouldPersistOrderWithItems() {
        // Arrange
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        order.setTotalPrice(new BigDecimal("125.00"));
        order.setPaymentId("pi_test_12345");
        order.setPaymentStatus("paid");
        order.setCreatedAt(Instant.now());
        order.setCreatedBy(customer.getEmail());

        OrderItem item1 = new OrderItem();
        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setPrice(new BigDecimal("50.00"));

        OrderItem item2 = new OrderItem();
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setPrice(new BigDecimal("75.00"));

        order.getOrderItems().add(item1);
        order.getOrderItems().add(item2);

        // Act
        Order savedOrder = orderRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Order foundOrder = orderRepository.findById(savedOrder.getOrderId()).orElseThrow();
        assertThat(foundOrder.getOrderId()).isNotNull();
        assertThat(foundOrder.getOrderItems()).hasSize(2);
        assertThat(foundOrder.getTotalPrice()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(foundOrder.getCustomer().getCustomerId()).isEqualTo(customer.getCustomerId());
    }

    // ==================== HELPER METHODS ====================

    private Order createAndPersistOrder(Customer customer, String status, Instant createdAt) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderStatus(status);
        order.setTotalPrice(new BigDecimal("99.99"));
        order.setPaymentId("pi_test_" + System.currentTimeMillis());
        order.setPaymentStatus("paid");
        order.setCreatedAt(createdAt);
        order.setCreatedBy(customer.getEmail());

        Order savedOrder = entityManager.persist(order);
        entityManager.flush();
        return savedOrder;
    }
}

 */