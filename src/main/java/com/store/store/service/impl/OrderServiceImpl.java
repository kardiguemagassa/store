package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.OrderItemReponseDto;
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
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProfileServiceImpl profileService;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

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
            order.setOrderItems(orderItems);

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

    // MÉTHODES VALIDATION MÉTIER
    private void validateOrderRequest(OrderRequestDto orderRequest) {
        if (orderRequest == null) {
            throw exceptionFactory.validationError("orderRequest",
                    getLocalizedMessage("validation.order.request.required"));
        }

        if (orderRequest.items() == null || orderRequest.items().isEmpty()) {
            throw exceptionFactory.validationError("items",
                    getLocalizedMessage("validation.order.items.required"));
        }

        if (orderRequest.totalPrice() == null || orderRequest.totalPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
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

    // MÉTHODES DE CRÉATION D'ENTITÉS
    private Order createOrderEntity(OrderRequestDto orderRequest, Customer customer) {
        Order order = new Order();
        order.setCustomer(customer);
        BeanUtils.copyProperties(orderRequest, order);
        order.setOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        return order;
    }

    private List<OrderItem> createOrderItems(OrderRequestDto orderRequest, Order order) {
        return orderRequest.items().stream().map(item -> {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Produit", "ID", item.productId().toString()));

            validateProductStock(product, item.quantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setPrice(item.price());
            return orderItem;
        }).collect(Collectors.toList());
    }

    private void validateProductStock(Product product, Integer quantity) {
        if (product.getPopularity() < quantity) {
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.order.insufficient.stock",
                            product.getName(), product.getPopularity(), quantity)
            );
        }
    }

    // MÉTHODES DE MAPPING
    private OrderResponseDto mapToOrderResponseDTO(Order order) {
        List<OrderItemReponseDto> itemDTOs = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponseDTO)
                .collect(Collectors.toList());

        return new OrderResponseDto(
                order.getOrderId(),
                order.getOrderStatus(),
                order.getTotalPrice(),
                order.getCreatedAt().toString(),
                itemDTOs
        );
    }

    private OrderItemReponseDto mapToOrderItemResponseDTO(OrderItem orderItem) {
        return new OrderItemReponseDto(
                orderItem.getProduct().getName(),
                orderItem.getQuantity(),
                orderItem.getPrice(),
                orderItem.getProduct().getImageUrl());
    }

    // MÉTHODE UTILITAIRE POUR L'INTERNATIONALISATION
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}