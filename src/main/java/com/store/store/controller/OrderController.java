package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.order.OrderRequestDto;
import com.store.store.dto.order.OrderResponseDto;
import com.store.store.dto.order.OrderValidationResultDto;
import com.store.store.service.IOrderService;

import com.store.store.service.impl.MessageServiceImpl;
import com.store.store.service.impl.OrderValidationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-01
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


    // VALIDATION PRÉ-CRÉATION
    @PostMapping("/validate")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Valider une commande",
            description = "Vérifie la validité d'une commande avant sa création (stock, cohérence des prix, paiement)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Validation effectuée avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données de commande invalides",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    public ResponseEntity<ApiResponse<OrderValidationResultDto>> validateOrder(
            @Valid @RequestBody OrderRequestDto request) {

        log.info("POST /api/v1/orders/validate - Validating order request");

        // Appel au service de validation
        OrderValidationResultDto result = validationService.validateOrder(request);

        // Message adapté selon le résultat
        String message;
        if (result.isValid()) {
            log.info("Order validation successful");
            message = messageService.getMessage("api.success.order.validation.passed");
        } else {
            log.warn("Order validation failed with {} errors", result.getErrorCount());
            message = messageService.getMessage(
                    "api.success.order.validation.failed",
                    result.getErrorCount()
            );
        }

        // Wrapper dans ApiResponse
        ApiResponse<OrderValidationResultDto> response = ApiResponse.success(message, result)
                .withPath("/api/v1/orders/validate");

        return ResponseEntity.ok(response);
    }

    // CRÉATION DE COMMANDE
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Créer une commande",
            description = "Crée une nouvelle commande après confirmation du paiement Stripe"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Commande créée avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données de commande invalides ou validation métier échouée",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Erreur serveur lors de la création"
            )
    })
    public ResponseEntity<ApiResponse<Void>> createOrder(
            @Valid @RequestBody OrderRequestDto request) {

        log.info("POST /api/v1/orders - Creating new order");

        // Validation métier avant création
        OrderValidationResultDto validation = validationService.validateOrder(request);

        if (!validation.isValid()) {
            log.warn("Order creation rejected: validation failed");

            String errorMessage = messageService.getMessage("api.error.order.validation.failed");

            ApiResponse<Void> errorResponse = ApiResponse.<Void>builder()
                    .success(false)
                    .message(errorMessage)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .path("/api/v1/orders")
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        // Créer la commande (le service retourne void)
        orderService.createOrder(request);

        log.info("Order created successfully");

        // Message de succès localisé
        String successMessage = messageService.getMessage("api.success.order.created");

        ApiResponse<Void> response = ApiResponse.<Void>created(successMessage, null)
                .withPath("/api/v1/orders");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // CONSULTATION DES COMMANDES
    @GetMapping("/customer")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Lister mes commandes",
            description = "Récupère toutes les commandes du client authentifié"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des commandes récupérée",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getCustomerOrders() {
        log.info("GET /api/v1/orders/customer - Fetching orders for authenticated customer");

        // Appel au service
        List<OrderResponseDto> orders = orderService.getCustomerOrders();

        log.info("Found {} orders for customer", orders.size());

        // Message de succès avec nombre de commandes
        String successMessage = messageService.getMessage(
                "api.success.orders.retrieved.count",
                orders.size()
        );

        ApiResponse<List<OrderResponseDto>> response = ApiResponse.success(successMessage, orders)
                .withPath("/api/v1/orders/customer");

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lister les commandes en attente",
            description = "Récupère toutes les commandes en attente de traitement (accès ADMIN)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des commandes en attente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (role ADMIN requis)"
            )
    })
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getAllPendingOrders() {
        log.info("GET /api/v1/orders - Fetching all pending orders (ADMIN)");

        // Appel au service
        List<OrderResponseDto> orders = orderService.getAllPendingOrders();

        log.info("Found {} pending orders", orders.size());

        // Message de succès avec nombre
        String successMessage = messageService.getMessage(
                "api.success.orders.pending.retrieved.count",
                orders.size()
        );

        ApiResponse<List<OrderResponseDto>> response = ApiResponse.success(successMessage, orders)
                .withPath("/api/v1/orders");

        return ResponseEntity.ok(response);
    }

    // MISE À JOUR DE COMMANDE
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Mettre à jour le statut d'une commande",
            description = "Change le statut d'une commande (CREATED, CONFIRMED, CANCELLED, DELIVERED)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Statut mis à jour avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Statut invalide ou transition non autorisée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Commande non trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (role ADMIN requis)"
            )
    })
    public ResponseEntity<ApiResponse<Void>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {

        log.info("PATCH /api/v1/orders/{}/status - Updating to: {}", orderId, status);

        // Appel au service (retourne void)
        orderService.updateOrderStatus(orderId, status);

        log.info("Order {} status updated successfully", orderId);

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.order.status.updated",
                orderId
        );

        ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/orders/" + orderId + "/status");

        return ResponseEntity.ok(response);
    }
}