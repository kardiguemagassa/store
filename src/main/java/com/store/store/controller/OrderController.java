package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.order.OrderFilterDto;
import com.store.store.dto.order.OrderRequestDto;
import com.store.store.dto.order.OrderResponseDto;
import com.store.store.dto.order.OrderValidationResultDto;
import com.store.store.entity.Customer;
import com.store.store.service.IOrderService;
import com.store.store.service.impl.MessageServiceImpl;
import com.store.store.service.impl.OrderValidationServiceImpl;
import com.store.store.service.impl.ProfileServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kardigué
 * @version 5.0 - Production Ready avec ApiResponse
 * @since 2025-23-11
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "API de gestion des commandes")
@SecurityRequirement(name = "Bearer Authentication")
public class OrderController {

    private final IOrderService orderService;
    private final OrderValidationServiceImpl validationService;
    private final MessageServiceImpl messageService;
    private final ProfileServiceImpl profileService;

    // ENDPOINTS CLIENT
    @PostMapping("/validate")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Valider une commande")
    public ResponseEntity<ApiResponse<OrderValidationResultDto>> validateOrder(
            @Valid @RequestBody OrderRequestDto request) {

        log.info("POST /api/v1/orders/validate - Validating order request");

        OrderValidationResultDto result = validationService.validateOrder(request);

        String message;
        if (result.isValid()) {
            log.info("Order validation successful");
            message = messageService.getMessage("api.success.order.validation.passed");
        } else {
            log.warn("Order validation failed with {} errors", Integer.valueOf(result.getErrorCount()));
            message = messageService.getMessage("api.success.order.validation.failed", Integer.valueOf(result.getErrorCount()));
        }

        ApiResponse<OrderValidationResultDto> response = ApiResponse.success(message, result)
                .withPath("/api/v1/orders/validate");

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Créer une commande")
    public ResponseEntity<ApiResponse<Void>> createOrder(
            @Valid @RequestBody OrderRequestDto request) {

        log.info("POST /api/v1/orders - Creating new order");

        OrderValidationResultDto validation = validationService.validateOrder(request);

        if (!validation.isValid()) {
            log.warn("Order creation rejected: validation failed");

            String errorMessage = messageService.getMessage("api.error.order.validation.failed");

            ApiResponse<Void> errorResponse = ApiResponse.<Void>builder()
                    .success(Boolean.FALSE)
                    .message(errorMessage)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .path("/api/v1/orders")
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        orderService.createOrder(request);

        log.info("Order created successfully");

        String successMessage = messageService.getMessage("api.success.order.created");

        ApiResponse<Void> response = ApiResponse.<Void>created(successMessage, null)
                .withPath("/api/v1/orders");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lister mes commandes")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getCustomerOrders() {
        log.info("GET /api/v1/orders/customer - Fetching orders for authenticated customer");

        List<OrderResponseDto> orders = orderService.getCustomerOrders();

        log.info("Found {} orders for customer", Integer.valueOf(orders.size()));

        String successMessage = messageService.getMessage("api.success.orders.retrieved.count", Integer.valueOf(orders.size()));

        ApiResponse<List<OrderResponseDto>> response = ApiResponse.success(successMessage, orders)
                .withPath("/api/v1/orders/customer");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Obtenir les détails d'une commande")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(@PathVariable Long orderId) {
        log.info("GET /api/v1/orders/{} - Fetching order details", orderId);

        try {
            OrderResponseDto order = orderService.getOrderById(orderId);

            String successMessage = messageService.getMessage("api.success.order.retrieved");
            ApiResponse<OrderResponseDto> response = ApiResponse.success(successMessage, order)
                    .withPath("/api/v1/orders/" + orderId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la commande {}", orderId, e);

            String errorMessage = messageService.getMessage("api.error.order.retrieval.failed");

            ApiResponse<OrderResponseDto> errorResponse = ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORDER_RETRIEVAL_ERROR",
                    errorMessage,
                    "/api/v1/orders/" + orderId
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/customer/paginated")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Lister mes commandes avec pagination")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerOrdersPaginated(
            @ParameterObject @ModelAttribute OrderFilterDto filters) {

        log.info("GET /api/v1/orders/customer/paginated - Filtres: {}", filters);

        try {
            // Récupérer le client authentifié
            Customer customer = profileService.getAuthenticatedCustomer();

            // Utiliser la méthode réutilisée
            Page<OrderResponseDto> ordersPage = orderService.findCustomerOrdersPaginated(customer.getCustomerId(), filters);

            // Créer la réponse paginée
            Map<String, Object> response = getPaginationResponse(ordersPage);

            String successMessage = messageService.getMessage("api.success.orders.retrieved.count", Long.valueOf(ordersPage.getTotalElements()));


            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(successMessage, response)
                    .withPath("/api/v1/orders/customer/paginated");

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des commandes client: {}", filters, e);

            String errorMessage = messageService.getMessage("api.error.orders.retrieval.failed");

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.badRequest(
                    "ORDER_FETCH_ERROR",
                    errorMessage,
                    "/api/v1/orders/customer/paginated"
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
        }
    }

    // ENDPOINTS ADMIN
    @GetMapping("/admin/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechercher des commandes (ADMIN)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchAdminOrders(
            @ParameterObject @ModelAttribute OrderFilterDto filters) {

        log.info("GET /api/v1/orders/admin/paginated - Filtres: {}", filters);

        try {
            // S'assurer que customerId est null pour voir toutes les commandes
            filters.setCustomerId(null);

            Page<OrderResponseDto> ordersPage = orderService.findOrdersWithFiltersPaginated(filters);

            Map<String, Object> response = getPaginationResponse(ordersPage);

            String successMessage;
            if (ordersPage.isEmpty()) {
                successMessage = messageService.getMessage("api.success.orders.filtered.empty");
            } else {
                successMessage = messageService.getMessage("api.success.orders.filtered.retrieved.count",
                        Long.valueOf(ordersPage.getTotalElements()));
            }

            ApiResponse<Map<String, Object>> apiResponse = ApiResponse.success(successMessage, response)
                    .withPath("/api/v1/orders/admin/paginated");

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des commandes avec filtres: {}", filters, e);

            String errorMessage = messageService.getMessage("api.error.orders.filtered.retrieval.failed");

            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORDERS_FILTER_ERROR",
                    errorMessage,
                    "/api/v1/orders/admin/paginated"
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les commandes en attente")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getAllPendingOrders() {
        log.info("GET /api/v1/orders/admin/pending - Fetching all pending orders");

        List<OrderResponseDto> orders = orderService.getAllPendingOrders();

        log.info("Found {} pending orders", Integer.valueOf(orders.size()));

        String successMessage = messageService.getMessage(
                "api.success.orders.pending.retrieved.count",
                Long.valueOf(orders.size()));

        ApiResponse<List<OrderResponseDto>> response = ApiResponse.success(successMessage, orders)
                .withPath("/api/v1/orders/admin/pending");

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour le statut d'une commande")
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        log.info("PATCH /api/v1/orders/{}/status - Updating to: {}", orderId, status);

        try {
            orderService.updateOrderStatus(orderId, status);

            log.info("Order {} status updated successfully", orderId);

            String successMessage = messageService.getMessage(
                    "api.success.order.status.updated",
                    orderId
            );

            ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                    .withPath("/api/v1/orders/" + orderId + "/status");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du statut de la commande {}", orderId, e);

            String errorMessage = messageService.getMessage("api.error.order.status.update.failed");

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORDER_STATUS_UPDATE_ERROR",
                    errorMessage,
                    "/api/v1/orders/" + orderId + "/status"
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PatchMapping("/admin/{orderId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Confirmer une commande")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable Long orderId) {
        log.info("PATCH /api/v1/orders/admin/{}/confirm", orderId);

        try {
            orderService.updateOrderStatus(orderId, "CONFIRMED");

            String successMessage = messageService.getMessage(
                    "api.success.order.confirmed",
                    orderId
            );
            ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                    .withPath("/api/v1/orders/admin/" + orderId + "/confirm");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de la confirmation de la commande {}", orderId, e);

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORDER_CONFIRM_ERROR",
                    messageService.getMessage("api.error.order.confirm.failed"),
                    "/api/v1/orders/admin/" + orderId + "/confirm"
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PatchMapping("/admin/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Annuler une commande")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> body) {

        log.info("PATCH /api/v1/orders/admin/{}/cancel", orderId);

        String reason = body != null ? body.get("reason") : null;
        log.info("Cancellation reason: {}", reason);

        try {
            orderService.updateOrderStatus(orderId, "CANCELLED");

            String successMessage = messageService.getMessage(
                    "api.success.order.cancelled",
                    orderId
            );
            ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                    .withPath("/api/v1/orders/admin/" + orderId + "/cancel");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erreur lors de l'annulation de la commande {}", orderId, e);

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "ORDER_CANCEL_ERROR",
                    messageService.getMessage("api.error.order.cancel.failed"),
                    "/api/v1/orders/admin/" + orderId + "/cancel"
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Map<String, Object> getPaginationResponse(Page<?> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", page.getContent());
        response.put("totalElements", Long.valueOf(page.getTotalElements()));
        response.put("totalPages", Long.valueOf(page.getTotalPages()));
        response.put("number", Long.valueOf(page.getNumber()));
        response.put("size", Integer.valueOf(page.getSize()));
        response.put("first", Boolean.valueOf(page.isFirst()));
        response.put("last", Boolean.valueOf(page.isLast()));
        response.put("numberOfElements", Integer.valueOf(page.getNumberOfElements()));
        return response;
    }
}