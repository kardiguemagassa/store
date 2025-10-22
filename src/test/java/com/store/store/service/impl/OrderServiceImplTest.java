package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.OrderRequestDto;
import com.store.store.dto.OrderResponseDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Order;
import com.store.store.entity.OrderItem;
import com.store.store.entity.Product;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.repository.OrderRepository;
import com.store.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProfileServiceImpl profileService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Customer customer;
    private Product product1;
    private Product product2;
    private OrderRequestDto orderRequestDto;
    private Order order;

    @BeforeEach
    void setUp() {
        // Setup customer
        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setName("John Doe");
        customer.setEmail("john.doe@example.com");

        // Setup products
        product1 = new Product();
        product1.setId(1L);
        product1.setName("Product 1");
        product1.setPrice(new BigDecimal("50.00"));
        product1.setImageUrl("https://example.com/image1.jpg");

        product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("75.00"));
        product2.setImageUrl("https://example.com/image2.jpg");

        // Setup order request DTO
        orderRequestDto = new OrderRequestDto(
                new BigDecimal("175.00"),
                "pi_test_123456",
                "paid",
                List.of(
                        new com.store.store.dto.OrderItemDto(1L, 2, new BigDecimal("50.00")),
                        new com.store.store.dto.OrderItemDto(2L, 1, new BigDecimal("75.00"))
                )
        );

        // Setup order
        order = new Order();
        order.setOrderId(1L);
        order.setCustomer(customer);
        order.setTotalPrice(new BigDecimal("175.00"));
        order.setPaymentId("pi_test_123456");
        order.setPaymentStatus("paid");
        order.setOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        order.setCreatedAt(Instant.now());

        // Setup order items
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setOrderItemId(1L);
        orderItem1.setOrder(order);
        orderItem1.setProduct(product1);
        orderItem1.setQuantity(2);
        orderItem1.setPrice(new BigDecimal("50.00"));

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOrderItemId(2L);
        orderItem2.setOrder(order);
        orderItem2.setProduct(product2);
        orderItem2.setQuantity(1);
        orderItem2.setPrice(new BigDecimal("75.00"));

        order.setOrderItems(List.of(orderItem1, orderItem2));
    }

    @Test
    @DisplayName("DEV-008: Créer une commande - Doit créer la commande avec succès")
    void createOrder_WithValidRequest_ShouldCreateOrder() {
        // Given
        when(profileService.getAuthenticatedCustomer()).thenReturn(customer);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.createOrder(orderRequestDto);

        // Then
        verify(profileService).getAuthenticatedCustomer();
        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("DEV-009: Créer une commande - Doit lancer une exception quand un produit n'existe pas")
    void createOrder_WithNonExistingProduct_ShouldThrowException() {
        // Given
        when(profileService.getAuthenticatedCustomer()).thenReturn(customer);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(orderRequestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product")
                .hasMessageContaining("'2'");

        verify(profileService).getAuthenticatedCustomer();
        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("DEV-010: Récupérer les commandes du client - Doit retourner la liste des commandes")
    void getCustomerOrders_WhenOrdersExist_ShouldReturnOrders() {
        // Given
        when(profileService.getAuthenticatedCustomer()).thenReturn(customer);
        when(orderRepository.findOrdersByCustomerWithNativeQuery(customer.getCustomerId()))
                .thenReturn(List.of(order));

        // When
        List<OrderResponseDto> result = orderService.getCustomerOrders();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);

        OrderResponseDto firstOrder = result.getFirst();
        assertThat(firstOrder.orderId()).isEqualTo(1L);
        assertThat(firstOrder.status()).isEqualTo(ApplicationConstants.ORDER_STATUS_CREATED);
        assertThat(firstOrder.totalPrice()).isEqualTo(new BigDecimal("175.00"));
        assertThat(firstOrder.items()).hasSize(2);

        // Vérification des items
        assertThat(firstOrder.items().getFirst().productName()).isEqualTo("Product 1");
        assertThat(firstOrder.items().get(0).quantity()).isEqualTo(2);
        assertThat(firstOrder.items().get(1).productName()).isEqualTo("Product 2");
        assertThat(firstOrder.items().get(1).quantity()).isEqualTo(1);

        verify(profileService).getAuthenticatedCustomer();
        verify(orderRepository).findOrdersByCustomerWithNativeQuery(customer.getCustomerId());
    }

    @Test
    @DisplayName("DEV-011: Récupérer les commandes du client - Doit retourner une liste vide quand aucune commande")
    void getCustomerOrders_WhenNoOrders_ShouldReturnEmptyList() {
        // Given
        when(profileService.getAuthenticatedCustomer()).thenReturn(customer);
        when(orderRepository.findOrdersByCustomerWithNativeQuery(customer.getCustomerId()))
                .thenReturn(List.of());

        // When
        List<OrderResponseDto> result = orderService.getCustomerOrders();

        // Then
        assertThat(result).isEmpty();
        verify(profileService).getAuthenticatedCustomer();
        verify(orderRepository).findOrdersByCustomerWithNativeQuery(customer.getCustomerId());
    }

    @Test
    @DisplayName("DEV-012: Récupérer toutes les commandes en attente - Doit retourner les commandes en attente")
    void getAllPendingOrders_WhenPendingOrdersExist_ShouldReturnOrders() {
        // Given
        when(orderRepository.findOrdersByStatusWithNativeQuery(ApplicationConstants.ORDER_STATUS_CREATED))
                .thenReturn(List.of(order));

        // When
        List<OrderResponseDto> result = orderService.getAllPendingOrders();

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);

        OrderResponseDto firstOrder = result.get(0);
        assertThat(firstOrder.status()).isEqualTo(ApplicationConstants.ORDER_STATUS_CREATED);
        assertThat(firstOrder.items()).hasSize(2);

        verify(orderRepository).findOrdersByStatusWithNativeQuery(ApplicationConstants.ORDER_STATUS_CREATED);
    }

    @Test
    @DisplayName("DEV-013: Mettre à jour le statut d'une commande - Doit mettre à jour le statut")
    void updateOrderStatus_WithValidData_ShouldUpdateStatus() {
        // Given
        Long orderId = 1L;
        String newStatus = "SHIPPED";

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        orderService.updateOrderStatus(orderId, newStatus);

        // Then
        verify(orderRepository).findById(orderId);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getOrderStatus()).isEqualTo(newStatus);
    }

    @Test
    @DisplayName("DEV-013b: Mettre à jour le statut d'une commande inexistante - Doit lancer une exception")
    void updateOrderStatus_WithNonExistingOrder_ShouldThrowException() {
        // Given
        Long orderId = 999L;
        String newStatus = "SHIPPED";

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, newStatus))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository).findById(orderId);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("DEV-014: Créer une commande avec des items vides - Doit gérer correctement")
    void createOrder_WithEmptyItems_ShouldCreateOrderWithNoItems() {
        // Given
        OrderRequestDto emptyOrderRequest = new OrderRequestDto(
                new BigDecimal("0.00"),
                "pi_test_empty",
                "paid",
                List.of()
        );

        when(profileService.getAuthenticatedCustomer()).thenReturn(customer);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // When
        orderService.createOrder(emptyOrderRequest);

        // Then
        verify(profileService).getAuthenticatedCustomer();
        verify(orderRepository).save(any(Order.class));
        // Vérifier que productRepository.findById n'est jamais appelé
        verify(productRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("DEV-015: Test d'intégration du mapping via getCustomerOrders")
    void getCustomerOrders_ShouldCorrectlyMapOrderToResponseDTO() {
        // Given
        when(profileService.getAuthenticatedCustomer()).thenReturn(customer);
        when(orderRepository.findOrdersByCustomerWithNativeQuery(customer.getCustomerId()))
                .thenReturn(List.of(order));

        // When
        List<OrderResponseDto> result = orderService.getCustomerOrders();

        // Then - Vérification complète du mapping
        OrderResponseDto responseDto = result.getFirst();

        // Vérification des propriétés de base
        assertThat(responseDto.orderId()).isEqualTo(order.getOrderId());
        assertThat(responseDto.status()).isEqualTo(order.getOrderStatus());
        assertThat(responseDto.totalPrice()).isEqualTo(order.getTotalPrice());
        assertThat(responseDto.createdAt()).isEqualTo(order.getCreatedAt().toString());

        // Vérification des items
        assertThat(responseDto.items()).hasSize(2);

        // Premier item
        assertThat(responseDto.items().getFirst().productName()).isEqualTo("Product 1");
        assertThat(responseDto.items().getFirst().quantity()).isEqualTo(2);
        assertThat(responseDto.items().getFirst().price()).isEqualTo(new BigDecimal("50.00"));
        assertThat(responseDto.items().getFirst().imageUrl()).isEqualTo("https://example.com/image1.jpg");

        // Deuxième item
        assertThat(responseDto.items().get(1).productName()).isEqualTo("Product 2");
        assertThat(responseDto.items().get(1).quantity()).isEqualTo(1);
        assertThat(responseDto.items().get(1).price()).isEqualTo(new BigDecimal("75.00"));
        assertThat(responseDto.items().get(1).imageUrl()).isEqualTo("https://example.com/image2.jpg");
    }
}