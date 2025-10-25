package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.OrderItemResponseDto;
import com.store.store.dto.OrderRequestDto;
import com.store.store.dto.OrderResponseDto;
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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
 * ✅ Service de gestion des commandes - Version corrigée avec conversion Instant → LocalDateTime
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProfileServiceImpl profileService;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

    // ✅ Constante pour la timezone Europe/Paris
    private static final ZoneId EUROPE_PARIS_ZONE = ZoneId.of("Europe/Paris");

    /**
     * Crée une nouvelle commande
     * @param orderRequest Les données de la commande
     * @throws BusinessException si la création échoue
     */
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

            // ✅ Utiliser la méthode helper au lieu de setOrderItems
            orderItems.forEach(order::addOrderItem);

            // Sauvegarde
            Order savedOrder = orderRepository.save(order);

            log.info("Order created successfully with ID: {}", savedOrder.getOrderId());

        } catch (DataAccessException e) {
            log.error("Database error while creating order", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.create.failed")
            );

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while creating order", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.order.create")
            );
        }
    }

    /**
     * Récupère les commandes du client connecté
     * @return Liste des commandes du client
     * @throws BusinessException si la récupération échoue
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getCustomerOrders() {
        try {
            log.info("Fetching orders for authenticated customer");

            Customer customer = profileService.getAuthenticatedCustomer();
            List<Order> orders = orderRepository.findOrdersByCustomerWithNativeQuery(customer.getCustomerId());

            log.info("Found {} orders for customer ID: {}", orders.size(), customer.getCustomerId());
            return orders.stream()
                    .map(this::mapToOrderResponseDTO)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching customer orders", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.fetch.customer.failed")
            );

        } catch (Exception e) {
            log.error("Unexpected error while fetching customer orders", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.order.fetch.customer")
            );
        }
    }

    /**
     * Récupère toutes les commandes en attente
     * @return Liste des commandes en attente
     * @throws BusinessException si la récupération échoue
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllPendingOrders() {
        try {
            log.info("Fetching all pending orders");

            List<Order> orders = orderRepository.findOrdersByStatusWithNativeQuery(ApplicationConstants.ORDER_STATUS_CREATED);

            log.info("Found {} pending orders", orders.size());
            return orders.stream()
                    .map(this::mapToOrderResponseDTO)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching pending orders", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.fetch.pending.failed")
            );

        } catch (Exception e) {
            log.error("Unexpected error while fetching pending orders", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.order.fetch.pending")
            );
        }
    }

    /**
     * Met à jour le statut d'une commande
     * @param orderId L'ID de la commande
     * @param newStatus Le nouveau statut
     * @throws OrderNotFoundException si la commande n'existe pas
     * @throws BusinessException si la mise à jour échoue
     */
    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        try {
            log.info("Updating status for order ID: {} to: {}", orderId, newStatus);

            // Validation des paramètres
            validateOrderUpdateParameters(orderId, newStatus);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> exceptionFactory.orderNotFound(orderId));

            // Validation métier de la transition de statut
            validateOrderStatusTransition(order.getOrderStatus(), newStatus);

            order.setOrderStatus(newStatus);
            orderRepository.save(order);

            log.info("Order ID: {} status successfully updated to: {}", orderId, newStatus);

        } catch (OrderNotFoundException e) {
            throw e;

        } catch (DataAccessException e) {
            log.error("Database error while updating order ID: {} status to: {}", orderId, newStatus, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.update.status.failed")
            );

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while updating order ID: {} status to: {}", orderId, newStatus, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.order.update.status")
            );
        }
    }

    // =====================================================
    // VALIDATION MÉTIER
    // =====================================================

    private void validateOrderRequest(OrderRequestDto orderRequest) {
        if (orderRequest == null) {
            throw exceptionFactory.validationError("orderRequest",
                    getLocalizedMessage("validation.order.request.required"));
        }

        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw exceptionFactory.validationError("items",
                    getLocalizedMessage("validation.order.items.required"));
        }

        if (orderRequest.getTotalPrice() == null || orderRequest.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw exceptionFactory.validationError("totalPrice",
                    getLocalizedMessage("validation.order.total.price.invalid"));
        }
    }

    private void validateOrderUpdateParameters(Long orderId, String newStatus) {
        if (orderId == null || orderId <= 0) {
            throw exceptionFactory.validationError("orderId",
                    getLocalizedMessage("validation.order.id.invalid"));
        }

        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw exceptionFactory.validationError("status",
                    getLocalizedMessage("validation.order.status.required"));
        }

        if (!isValidOrderStatus(newStatus)) {
            throw exceptionFactory.validationError("status",
                    getLocalizedMessage("validation.order.status.invalid", newStatus));
        }
    }

    private void validateOrderStatusTransition(String currentStatus, String newStatus) {
        if (ApplicationConstants.ORDER_STATUS_CANCELLED.equals(currentStatus)) {
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.cannot.update.cancelled")
            );
        }

        if (ApplicationConstants.ORDER_STATUS_DELIVERED.equals(currentStatus) &&
                !ApplicationConstants.ORDER_STATUS_DELIVERED.equals(newStatus)) {
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.cannot.update.delivered")
            );
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
                    getLocalizedMessage("error.order.insufficient.stock",
                            product.getName(), product.getPopularity(), quantity)
            );
        }
    }

    // =====================================================
    // CRÉATION D'ENTITÉS
    // =====================================================

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
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Produit", "ID", item.productId().toString()
                    ));

            validateProductStock(product, item.quantity());

            OrderItem orderItem = new OrderItem();
            // ✅ Ne pas définir order ici, la méthode addOrderItem le fera
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setPrice(item.price());
            return orderItem;
        }).collect(Collectors.toList());
    }

    // =====================================================
    // MAPPING DTO (AVEC CONVERSION INSTANT → LOCALDATETIME) ✅
    // =====================================================

    /**
     * ✅ Mappe une Order entity vers OrderResponseDto
     * Convertit Instant → LocalDateTime pour la zone Europe/Paris
     */
    private OrderResponseDto mapToOrderResponseDTO(Order order) {
        // 1. Mapper les items
        List<OrderItemResponseDto> itemDTOs = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponseDTO)
                .collect(Collectors.toList());

        // 2. Construire le DTO avec builder et conversion des dates
        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .totalPrice(order.getTotalPrice())
                .paymentIntentId(order.getPaymentIntentId())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(toLocalDateTime(order.getCreatedAt()))   // ✅ Conversion Instant → LocalDateTime
                .updatedAt(toLocalDateTime(order.getUpdatedAt()))   // ✅ Conversion Instant → LocalDateTime
                .items(itemDTOs)
                .build();
    }

    /**
     * ✅ Mappe un OrderItem entity vers OrderItemResponseDto
     */
    private OrderItemResponseDto mapToOrderItemResponseDTO(OrderItem orderItem) {
        Product product = orderItem.getProduct();

        // Calculer le subtotal
        BigDecimal subtotal = orderItem.getPrice()
                .multiply(BigDecimal.valueOf(orderItem.getQuantity()));

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

    // =====================================================
    // UTILITAIRES
    // =====================================================

    /**
     * ✅ Convertit Instant en LocalDateTime pour la zone Europe/Paris
     *
     * @param instant L'instant à convertir (peut être null)
     * @return LocalDateTime correspondant ou null si instant est null
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, EUROPE_PARIS_ZONE);
    }

    /**
     * Récupère un message localisé
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}