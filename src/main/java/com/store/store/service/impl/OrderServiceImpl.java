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
 * Implementation of the IOrderService interface for managing customer orders.
 * This class provides methods to create orders, retrieve orders, and update their statuses.
 * It handles various validations and ensures consistent handling of business rules and exceptions.
 *
 * Dependencies:
 * - orderRepository: Handles database operations for orders.
 * - productRepository: Manages product data, including stock validation.
 * - profileService: Provides user profile information and related operations.
 * - exceptionFactory: Generates custom exceptions for error management.
 * - messageSource: Retrieves localized messages for internationalization.
 * - EUROPE_PARIS_ZONE: Time zone for date and time transformations.
 * - log: Logger for logging events or errors.
 *
 * @author Kardigué
 *  * @version 3.0
 *  * @since 2025-11-01
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

    // Constante pour la timezone Europe/Paris
    private static final ZoneId EUROPE_PARIS_ZONE = ZoneId.of("Europe/Paris");

    /**
     * Creates a new order for the authenticated customer based on the provided order request.
     * This method validates the input, generates the order, its associated items, and saves it to the database.
     * Handles various exceptions and logs appropriate messages about the creation process.
     *
     * @param orderRequest DTO containing the details of the order to create, including items and other necessary information.
     * @throws BusinessException if the input validation fails or if a business-related error occurs during order creation.
     * @throws DataAccessException if there is a database-related error while creating the order.
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

            // Utiliser la méthode helper au lieu de setOrderItems
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
     * Retrieves the list of orders associated with the authenticated customer.
     * Fetches order data from the database, maps it to a list of OrderResponseDto, and returns it.
     * Handles various exceptions such as database access errors and unexpected issues.
     *
     * @return a list of {@code OrderResponseDto} representing the orders of the authenticated customer.
     * @throws BusinessException if a business-related error occurs, such as failure to fetch orders.
     * @throws DataAccessException if a database-related error occurs during the process.
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
     * Retrieves all pending orders from the database.
     * This method fetches orders with the status "CREATED" and maps them to a list
     * of OrderResponseDto objects. It handles exceptions related to database access
     * issues or unexpected errors during the retrieval process.
     *
     * @return a list of {@code OrderResponseDto} representing all pending orders.
     * @throws BusinessException if a business-related error occurs while fetching orders.
     * @throws DataAccessException if there is a database-related error during the operation.
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
     * Updates the status of an existing order identified by its ID.
     * Validates the input parameters, ensures the status transition is allowed,
     * and persists the updated order status in the database.
     * Handles various exceptions, including validation, database access, and unexpected errors.
     *
     * @param orderId the unique identifier of the order to update
     * @param newStatus the new status to set for the specified order
     * @throws OrderNotFoundException if the order with the given ID does not exist
     * @throws IllegalArgumentException if the provided parameters are invalid
     * @throws BusinessException if the status transition is not valid or
     *         if other business-related errors occur
     * @throws DataAccessException if there is an error accessing the database
     */
    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        try {
            log.info("Updating status for order ID: {} to: {}", orderId, newStatus);

            // Validation des paramètres
            validateOrderUpdateParameters(orderId, newStatus);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Product", "id", orderId.toString()));

            // Validation métier de la transition de statut
            validateOrderStatusTransition(order.getOrderStatus(), newStatus);

            order.setOrderStatus(newStatus);
            orderRepository.save(order);

            log.info("Order ID: {} status successfully updated to: {}", orderId, newStatus);

        } catch (OrderNotFoundException | BusinessException e) {
            throw e;

        } catch (DataAccessException e) {
            log.error("Database error while updating order ID: {} status to: {}", orderId, newStatus, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.update.status.failed")
            );

        } catch (Exception e) {
            log.error("Unexpected error while updating order ID: {} status to: {}", orderId, newStatus, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.order.update.status")
            );
        }
    }

    // VALIDATION MÉTIER

    /**
     * Validates the provided order request to ensure all required fields are present and valid.
     * Throws an exception if any validation checks fail.
     *
     * @param orderRequest the data transfer object containing the details of the order to validate.
     *                     Must not be null and should include valid items and a total price.
     */
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

    /**
     * Validates the input parameters for updating an order. Ensures that the order ID
     * is positive and not null, the new status is not null or empty, and that the status
     * is among the valid predefined order statuses.
     *
     * @param orderId the unique identifier of the order to validate. Must not be null
     *                and must be greater than zero.
     * @param newStatus the new status to set for the order. Must not be null, empty,
     *                  or invalid based on the predefined valid statuses.
     */
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

    /**
     * Validates a transition between two order statuses to ensure the transition is allowed.
     * Throws a business error if the current status is "CANCELLED" or if an invalid transition
     * is attempted from a "DELIVERED" status to any other status.
     *
     * @param currentStatus the current status of the order. Must not be null or empty and
     *                      should match one of the predefined statuses in the system.
     * @param newStatus the new status to which the order is transitioning. Must not be null
     *                  or empty and should be a valid predefined status.
     * @throws BusinessException if the transition from the current status to the new status
     *                           is not allowed.
     */
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

    /**
     * Checks if the provided order status is valid by comparing it against predefined valid order statuses.
     *
     * @param status the order status to validate. Must not be null or empty and should match one of the
     *               valid statuses defined in the system.
     * @return true if the order status is valid; false otherwise.
     */
    private boolean isValidOrderStatus(String status) {
        return ApplicationConstants.ORDER_STATUS_CREATED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_CONFIRMED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_CANCELLED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_DELIVERED.equals(status);
    }

    /**
     * Validates the stock availability for a given product and quantity.
     * Ensures that the requested quantity does not exceed the product's available stock.
     * If the stock is insufficient, a business error is thrown.
     *
     * @param product the product to validate stock for. Must not be null and should include a valid stock value.
     * @param quantity the requested quantity to check against the product's available stock.
     *                 Must be a non-negative integer.
     * @throws BusinessException if the product's available stock is less than the requested quantity.
     */
    private void validateProductStock(Product product, Integer quantity) {
        // Note : popularity utilisé comme stock temporairement
        if (product.getPopularity() < quantity) {
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.insufficient.stock",
                            product.getName(), product.getPopularity(), quantity)
            );
        }
    }

    // CRÉATION D'ENTITÉS

    /**
     * Creates an Order entity based on the provided OrderRequestDto and Customer.
     * This method initializes a new Order, sets its attributes based on
     * the input data, and assigns default values for certain fields.
     *
     * @param orderRequest the data transfer object containing the order details, including total price,
     *                     payment intent ID, and payment status
     * @param customer the customer associated with the order
     * @return the created Order entity with all fields populated
     */
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
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setPrice(item.price());
            return orderItem;
        }).collect(Collectors.toList());
    }

    // MAPPING DTO (AVEC CONVERSION INSTANT → LOCALDATETIME)

    /**
     * Maps an Order entity to an OrderResponseDto object.
     *
     * @param order the Order entity to be mapped
     * @return an OrderResponseDto containing the data from the provided Order entity
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
                .createdAt(toLocalDateTime(order.getCreatedAt()))   // Conversion Instant → LocalDateTime
                .updatedAt(toLocalDateTime(order.getUpdatedAt()))   // Conversion Instant → LocalDateTime
                .items(itemDTOs)
                .build();
    }

    /**
     * Maps an OrderItem entity to an OrderItemResponseDto.
     *
     * @param orderItem the OrderItem entity to be mapped
     * @return an OrderItemResponseDto containing the mapped data
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

    // UTILITAIRES

    /**
     * Converts a given {@code Instant} to a {@code LocalDateTime} using the {@code EUROPE_PARIS_ZONE} time zone.
     *
     * @param instant the {@code Instant} to be converted. Can be null, in which case the method returns null.
     * @return the converted {@code LocalDateTime} if the input is not null; otherwise, null.
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, EUROPE_PARIS_ZONE);
    }

    /**
     * Retrieves a localized message based on the provided message code and arguments.
     * This method fetches the message specific to the current locale.
     *
     * @param code the message code used to identify the localized message
     * @param args the arguments that will be used to replace placeholders in the message
     * @return the localized message as a string
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}