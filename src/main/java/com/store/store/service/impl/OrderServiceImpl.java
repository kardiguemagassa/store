package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.order.OrderFilterDto;
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

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Kardigué
 * @version 5.0 - Production Ready avec ApiResponse
 * @since 2025-23-11
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

            validateOrderRequest(orderRequest);

            Customer customer = profileService.getAuthenticatedCustomer();

            Order order = createOrderEntity(orderRequest, customer);
            List<OrderItem> orderItems = createOrderItems(orderRequest, order);

            orderItems.forEach(order::addOrderItem);

            Order savedOrder = orderRepository.save(order);

            log.info("Order created successfully with ID: {}", savedOrder.getOrderId());

        } catch (DataAccessException e) {
            log.error("Database error while creating order", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.order.create.failed"));

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while creating order", e);
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

            // CORRECTION: Utiliser Integer.valueOf() pour convertir en Object
            log.info("Found {} orders for customer ID: {}", Integer.valueOf(orders.size()), customer.getCustomerId());
            return orders.stream().map(this::mapToOrderResponseDTO).collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching customer orders", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.order.fetch.customer.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while fetching customer orders", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.fetch.customer"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        try {
            log.info("Fetching order by ID: {}", orderId);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Order", "id", orderId.toString()));

            return mapToOrderResponseDTO(order);

        } catch (OrderNotFoundException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while fetching order by ID: {}", orderId, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.fetch"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllPendingOrders() {
        try {
            log.info("Fetching all pending orders");
            List<Order> orders = orderRepository.findOrdersByStatusWithNativeQuery(ApplicationConstants.ORDER_STATUS_CREATED);

            log.info("Found {} pending orders", Integer.valueOf(orders.size()));
            return orders.stream().map(this::mapToOrderResponseDTO).collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching pending orders", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.order.fetch.pending.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while fetching pending orders", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.fetch.pending"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        try {
            log.info("Fetching all orders for admin");

            List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

            log.info("Found {} total orders", Optional.of(orders.size()));
            return orders.stream()
                    .map(this::mapToOrderResponseDTO)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching all orders", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.order.fetch.all.failed"));
        } catch (Exception e) {
            log.error("Unexpected error while fetching all orders", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.fetch.all"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> findOrdersWithFiltersPaginated(OrderFilterDto filters) {
        try {
            log.info("Fetching orders with filters (paginated): {}", filters);

            Specification<Order> spec = buildSpecification(filters);

            Pageable pageable = PageRequest.of(
                    filters.getPage() != null ? filters.getPage() : 0,
                    filters.getSize() != null ? filters.getSize() : 10,
                    buildSort(filters)
            );

            Page<Order> ordersPage = orderRepository.findAll(spec, pageable);

            // CORRECTION: Convertir tous les int en Integer
            log.info("Found {} orders on page {} of {}",
                    Integer.valueOf(ordersPage.getNumberOfElements()),
                    Integer.valueOf(ordersPage.getNumber()),
                    Integer.valueOf(ordersPage.getTotalPages()));

            return ordersPage.map(this::mapToOrderResponseDTO);

        } catch (DataAccessException e) {
            log.error("Database error while fetching orders with filters: {}", filters, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.order.fetch.filtered.failed"));
        } catch (Exception e) {
            log.error("Unexpected error while fetching orders with filters: {}", filters, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.fetch.filtered"));
        }
    }

    // Réutilise la méthode existante pour les clients
    @Transactional(readOnly = true)
    public Page<OrderResponseDto> findCustomerOrdersPaginated(Long customerId, OrderFilterDto filters) {
        try {
            log.info("Fetching paginated orders for customer ID: {}", customerId);

            // Créer une copie des filtres avec le customerId
            OrderFilterDto customerFilters = OrderFilterDto.builder()
                    .page(filters.getPage())
                    .size(filters.getSize())
                    .sortBy(filters.getSortBy())
                    .sortDirection(filters.getSortDirection())
                    .status(filters.getStatus())
                    // Note: on ne copie pas 'query' car c'est pour l'admin
                    .customerId(customerId)
                    .build();

            // RÉUTILISATION de la méthode existante !
            return findOrdersWithFiltersPaginated(customerFilters);

        } catch (DataAccessException e) {
            log.error("Database error while fetching customer orders for customer: {}", customerId, e);
            throw new BusinessException("Erreur lors de la récupération de vos commandes");
        }
    }

    // MISE À JOUR DE COMMANDE
    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String newStatus) {
        try {
            log.info("Updating status for order ID: {} to: {}", orderId, newStatus);

            validateOrderUpdateParameters(orderId, newStatus);

            Order order = orderRepository.findById(orderId).orElseThrow(() ->
                    exceptionFactory.resourceNotFound("Order", "id", orderId.toString()));

            validateOrderStatusTransition(order.getOrderStatus(), newStatus);

            order.setOrderStatus(newStatus);
            orderRepository.save(order);

            log.info("Order ID: {} status successfully updated to: {}", orderId, newStatus);

        } catch (OrderNotFoundException | BusinessException e) {
            throw e;

        } catch (DataAccessException e) {
            log.error("Database error while updating order ID: {} status to: {}", orderId, newStatus, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.order.update.status.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while updating order ID: {} status to: {}", orderId, newStatus, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.unexpected.order.update.status"));
        }
    }

    // VALIDATION MÉTIER
    private void validateOrderRequest(OrderRequestDto orderRequest) {
        if (orderRequest == null) {
            throw exceptionFactory.validationError("orderRequest",
                    messageService.getMessage("validation.order.request.required"));
        }
        if (orderRequest.getItems() == null || orderRequest.getItems().isEmpty()) {
            throw exceptionFactory.validationError("items",
                    messageService.getMessage("validation.order.items.required"));
        }
        if (orderRequest.getTotalPrice() == null || orderRequest.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw exceptionFactory.validationError("totalPrice",
                    messageService.getMessage("validation.order.total.price.invalid"));
        }
    }

    private void validateOrderUpdateParameters(Long orderId, String newStatus) {
        if (orderId == null || orderId <= 0) {
            throw exceptionFactory.validationError("orderId",
                    messageService.getMessage("validation.order.id.invalid"));
        }
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw exceptionFactory.validationError("status",
                    messageService.getMessage("validation.order.status.required"));
        }
        if (!isValidOrderStatus(newStatus)) {
            throw exceptionFactory.validationError("status",
                    messageService.getMessage("validation.order.status.invalid", newStatus));
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
            throw exceptionFactory.businessError(
                    messageService.getMessage("error.order.cannot.update.delivered"));
        }
    }

    private boolean isValidOrderStatus(String status) {
        return ApplicationConstants.ORDER_STATUS_CREATED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_CONFIRMED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_CANCELLED.equals(status) ||
                ApplicationConstants.ORDER_STATUS_DELIVERED.equals(status);
    }

    private void validateProductStock(Product product, Integer quantity) {
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
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Product", "ID", item.productId().toString()));

            validateProductStock(product, item.quantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(item.quantity());
            orderItem.setPrice(item.price());
            return orderItem;
        }).collect(Collectors.toList());
    }

    // SPÉCIFICATION POUR LES FILTRES
    private Specification<Order> buildSpecification(OrderFilterDto filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // NOUVEAU: Filtre par customer ID (si fourni)
            if (filters.getCustomerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("customer").get("id"), filters.getCustomerId()));
            }

            // Filtre par statut
            if (filters.getStatus() != null && !filters.getStatus().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("orderStatus"), filters.getStatus()));
            }

            //Filtre par Query
            if (filters.getQuery() != null && !filters.getQuery().isBlank()) {
                String searchTerm = "%" + filters.getQuery().toLowerCase() + "%";

                // Recherche par email du client
                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customer").get("email")),
                        searchTerm
                );

                // Recherche par nom du client
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customer").get("name")),
                        searchTerm
                );

                // Recherche par ID de commande
                try {
                    Long orderId = Long.parseLong(filters.getQuery());
                    Predicate idPredicate = criteriaBuilder.equal(root.get("orderId"), orderId);

                    predicates.add(criteriaBuilder.or(emailPredicate, namePredicate, idPredicate));
                } catch (NumberFormatException e) {
                    // Si ce n'est pas un nombre, on cherche juste par email et nom
                    predicates.add(criteriaBuilder.or(emailPredicate, namePredicate));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(OrderFilterDto filters) {
        if (filters.getSortBy() == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Sort.Direction direction = Sort.Direction.fromString(
                filters.getSortDirection() != null ? filters.getSortDirection() : "DESC"
        );

        return Sort.by(direction, filters.getSortBy());
    }

    // MAPPING DTO
    private OrderResponseDto mapToOrderResponseDTO(Order order) {
        List<OrderItemResponseDto> itemDTOs = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponseDTO)
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .totalPrice(order.getTotalPrice())
                .paymentIntentId(order.getPaymentIntentId())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(toLocalDateTime(order.getCreatedAt()))
                .updatedAt(toLocalDateTime(order.getUpdatedAt()))
                // Informations client
                .customerEmail(order.getCustomer() != null ? order.getCustomer().getEmail() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getName() : null)
                .items(itemDTOs)
                .build();
    }

    private OrderItemResponseDto mapToOrderItemResponseDTO(OrderItem orderItem) {
        Product product = orderItem.getProduct();

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

    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, EUROPE_PARIS_ZONE);
    }
}