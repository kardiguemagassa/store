package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.order.OrderItemResponseDto;
import com.store.store.dto.order.OrderRequestDto;
import com.store.store.dto.order.OrderResponseDto;
import com.store.store.entity.Customer;
import com.store.store.entity.Order;
import com.store.store.entity.OrderItem;
import com.store.store.entity.Product;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.exception.OrderNotFoundException;
import com.store.store.repository.OrderRepository;
import com.store.store.repository.ProductRepository;
import com.store.store.service.IOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProfileServiceImpl profileService;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    private static final ZoneId EUROPE_PARIS_ZONE = ZoneId.of("Europe/Paris");

    // CRÉATION DE COMMANDE

    @Override
    @Transactional
    public void createOrder(OrderRequestDto orderRequest) {
        try {
            log.info("Creating new order for customer");

            // Validation des données d'entrée
            validateOrderRequest(orderRequest);

            Customer customer = profileService.getAuthenticatedCustomer();

            // Création de la commande
            Order order = createOrderEntity(orderRequest, customer);

            // Création des items de commande
            List<OrderItem> orderItems = createOrderItems(orderRequest, order);

            // Utiliser la méthode helper au lieu de setOrderItems
            orderItems.forEach(order::addOrderItem);

            // Sauvegarde
            Order savedOrder = orderRepository.save(order);

            log.info("Order created successfully with ID: {}", savedOrder.getOrderId());

        } catch (DataAccessException e) {
            log.error("Database error while creating order", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.order.create.failed"));

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while creating order", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.create"));
        }
    }

    // CONSULTATION DES COMMANDES

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getCustomerOrders() {
        try {
            log.info("Fetching orders for authenticated customer");

            Customer customer = profileService.getAuthenticatedCustomer();
            List<Order> orders = orderRepository.findOrdersByCustomerWithNativeQuery(customer.getCustomerId());

            log.info("Found {} orders for customer ID: {}", orders.size(), customer.getCustomerId());
            return orders.stream().map(this::mapToOrderResponseDTO).collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching customer orders", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.order.fetch.customer.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while fetching customer orders", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.fetch.customer"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllPendingOrders() {
        try {
            log.info("Fetching all pending orders");
            List<Order> orders = orderRepository.findOrdersByStatusWithNativeQuery(ApplicationConstants.ORDER_STATUS_CREATED);

            log.info("Found {} pending orders", orders.size());
            return orders.stream().map(this::mapToOrderResponseDTO).collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching pending orders", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.order.fetch.pending.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while fetching pending orders", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.fetch.pending"));
        }
    }

    // MISE À JOUR DE COMMANDE

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        try {
            log.info("Updating status for order ID: {} to: {}", orderId, newStatus);

            // Validation des paramètres
            validateOrderUpdateParameters(orderId, newStatus);

            Order order = orderRepository.findById(orderId).orElseThrow(() -> exceptionFactory.resourceNotFound(
                    "Order", "id", orderId.toString()));

            // Validation métier de la transition de statut
            validateOrderStatusTransition(order.getOrderStatus(), newStatus);

            order.setOrderStatus(newStatus);
            orderRepository.save(order);

            log.info("Order ID: {} status successfully updated to: {}", orderId, newStatus);

        } catch (OrderNotFoundException | BusinessException e) {
            throw e;

        } catch (DataAccessException e) {
            log.error("Database error while updating order ID: {} status to: {}", orderId, newStatus, e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.order.update.status.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while updating order ID: {} status to: {}", orderId, newStatus, e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.update.status"));
        }
    }

    // VALIDATION MÉTIER

    private void validateOrderRequest(OrderRequestDto orderRequest) {
        if (orderRequest == null) {
            throw exceptionFactory.validationError("orderRequest", messageService.getMessage("validation.order.request.required"));
        }
        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw exceptionFactory.validationError("items", messageService.getMessage("validation.order.items.required"));
        }
        if (orderRequest.getTotalPrice() == null || orderRequest.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw exceptionFactory.validationError("totalPrice", messageService.getMessage("validation.order.total.price.invalid"));
        }
    }


    private void validateOrderUpdateParameters(Long orderId, String newStatus) {
        if (orderId == null || orderId <= 0) {
            throw exceptionFactory.validationError("orderId", messageService.getMessage("validation.order.id.invalid"));
        }
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw exceptionFactory.validationError("status", messageService.getMessage("validation.order.status.required"));
        }
        if (!isValidOrderStatus(newStatus)) {
            throw exceptionFactory.validationError("status", messageService.getMessage("validation.order.status.invalid", newStatus));
        }
    }

    private void validateOrderStatusTransition(String currentStatus, String newStatus) {
        if (ApplicationConstants.ORDER_STATUS_CANCELLED.equals(currentStatus)) {

            throw exceptionFactory.businessError(
                    messageService.getMessage("error.order.cannot.update.cancelled")
            );
        }

        if (ApplicationConstants.ORDER_STATUS_DELIVERED.equals(currentStatus) &&
                !ApplicationConstants.ORDER_STATUS_DELIVERED.equals(newStatus)) {
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.order.cannot.update.delivered"));
        }
    }

    private boolean isValidOrderStatus(String status) {
        return ApplicationConstants.ORDER_STATUS_CREATED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_CONFIRMED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_CANCELLED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_DELIVERED.equals(status);
    }


    private void validateProductStock(Product product, Integer quantity) {
        // Note : popularity utilisé comme stock temporairement
        if (product.getPopularity() < quantity) {

            throw exceptionFactory.businessError(
                    messageService.getMessage("error.order.insufficient.stock",
                            product.getName(), product.getPopularity(), quantity)
            );
        }
    }

    // CRÉATION D'ENTITÉS

    private Order createOrderEntity(OrderRequestDto orderRequest, Customer customer) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setTotalPrice(orderRequest.getTotalPrice());
        order.setPaymentIntentId(orderRequest.getPaymentIntentId());
        order.setPaymentStatus(orderRequest.getPaymentStatus());
        order.setOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        return order;
    }

    private List<OrderItem> createOrderItems(OrderRequestDto orderRequest, Order order) {
        return orderRequest.getItems().stream().map(item -> {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Product", "ID", item.productId().toString()));

            validateProductStock(product, item.quantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setPrice(item.price());
            return orderItem;
        }).collect(Collectors.toList());
    }


    // MAPPING DTO (AVEC CONVERSION INSTANT → LOCALDATETIME)

    private OrderResponseDto mapToOrderResponseDTO(Order order) {
        // 1. Mapper les items
        List<OrderItemResponseDto> itemDTOs = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponseDTO).collect(Collectors.toList());

        // 2. Construire le DTO avec builder et conversion des dates
        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .totalPrice(order.getTotalPrice())
                .paymentIntentId(order.getPaymentIntentId())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(toLocalDateTime(order.getCreatedAt()))   // Conversion Instant → LocalDateTime
                .updatedAt(toLocalDateTime(order.getUpdatedAt()))   // Conversion Instant → LocalDateTime
                .items(itemDTOs)
                .build();
    }

    private OrderItemResponseDto mapToOrderItemResponseDTO(OrderItem orderItem) {
        Product product = orderItem.getProduct();

        // Calculer le subtotal
        BigDecimal subtotal = orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));

        return OrderItemResponseDto.builder()
                .orderItemId(orderItem.getOrderItemId())
                .productId(product.getId())
                .productName(product.getName())
                .productImageUrl(product.getImageUrl())
                .quantity(orderItem.getQuantity())
                .price(orderItem.getPrice())
                .subtotal(subtotal)
                .build();
    }

    // UTILITAIRES

    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, EUROPE_PARIS_ZONE);
    }

}