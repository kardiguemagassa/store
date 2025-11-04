package com.store.store.controller;

import com.store.store.dto.*;
import com.store.store.service.IOrderService;
import com.store.store.service.impl.OrderValidationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing endpoints for managing customer orders.
 *
 * This class defines endpoints to validate, create, retrieve, and update orders.
 * It includes mechanisms for both user and admin-specific functionalities, including order
 * validation, listing customer orders, creating an order after payment confirmation, listing all
 * pending orders for admins, and updating order statuses.
 *
 * Role-based access control is implemented using annotations to ensure appropriate access to sensitive operations.
 *
 * @author Kardigué
 *  * @version 3.0
 *  * @since 2025-11-01
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


    // VALIDATION PRE-CREATION

    /**
     * Validates an order to ensure it is ready for creation. The validation process checks
     * stock availability, price consistency, and payment details.
     *
     * @param request the order request object containing details of the order to be validated
     * @return a {@code ResponseEntity} containing the validation results, including whether the
     *         order is valid and any associated errors or warnings
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Valider une commande",
            description = "Vérifie la validité d'une commande avant sa création (stock, cohérence des prix, paiement)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Validation effectuée avec succès",
                    content = @Content(schema = @Schema(implementation = OrderValidationResultDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données de commande invalides",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    public ResponseEntity<OrderValidationResultDto> validateOrder(
            @Valid @RequestBody OrderRequestDto request
    ) {
        log.info("Validating order request");

        OrderValidationResultDto result = validationService.validateOrder(request);

        if (result.isValid()) {
            log.info("Order validation successful");
        } else {
            log.warn("Order validation failed with {} errors", result.getErrorCount());
        }

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // CRÉATION DE COMMANDE
    // =====================================================

    /**
     * Creates a new order after validating the request and confirming payment.
     * This method validates the order information, ensures compliance with
     * business rules, and upon successful validation, creates the order.
     *
     * @param request the order details provided by the user, encapsulated in
     *                an {@code OrderRequestDto} object
     * @return a {@code ResponseEntity} containing a {@code SuccessResponseDto}.
     *         The response includes the status of the creation process, either
     *         success or failure with an appropriate message.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Créer une commande",
            description = "Crée une nouvelle commande après confirmation du paiement Stripe"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Commande créée avec succès",
                    content = @Content(schema = @Schema(implementation = SuccessResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Données de commande invalides ou validation métier échouée",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erreur serveur lors de la création"
            )
    })
    public ResponseEntity<SuccessResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto request
    ) {
        log.info("Creating new order");

        // Validation métier avant création
        OrderValidationResultDto validation = validationService.validateOrder(request);

        if (!validation.isValid()) {
            log.warn("Order creation rejected: validation failed");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(SuccessResponseDto.builder()
                            .success(false)
                            .message("La commande n'est pas valide")
                            .build());
        }

        // Créer la commande
        orderService.createOrder(request);

        log.info("Order created successfully");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SuccessResponseDto.builder()
                        .success(true)
                        .message("Commande créée avec succès")
                        .build());
    }

    // CONSULTATION DES COMMANDES

    /**
     * Retrieves all orders associated with the authenticated customer.
     *
     * @return a {@code ResponseEntity} containing a list of {@code OrderResponseDto} objects
     *         representing the customer's orders.
     */
    @GetMapping("/customer")
    @PreAuthorize("hasRole('USER')")
    @Operation(
            summary = "Lister mes commandes",
            description = "Récupère toutes les commandes du client authentifié"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des commandes récupérée",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    public ResponseEntity<List<OrderResponseDto>> getCustomerOrders() {
        log.info("Fetching orders for authenticated customer");

        List<OrderResponseDto> orders = orderService.getCustomerOrders();

        log.info("Found {} orders for customer", orders.size());

        return ResponseEntity.ok(orders);
    }

    /**
     * Retrieves all pending orders for administration purposes.
     * This method is intended for use by users with the ADMIN role to view
     * all orders that are awaiting processing.
     *
     * @return a {@code ResponseEntity} containing a list of {@code OrderResponseDto} objects
     *         representing pending orders. The response will include an HTTP 200 status code
     *         on success or appropriate error codes otherwise.
     */
    @GetMapping
    //@PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Lister les commandes en attente",
            description = "Récupère toutes les commandes en attente de traitement (accès ADMIN)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des commandes en attente",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (role ADMIN requis)"
            )
    })
    public ResponseEntity<List<OrderResponseDto>> getAllPendingOrders() {
        log.info("Fetching all pending orders (ADMIN)");

        List<OrderResponseDto> orders = orderService.getAllPendingOrders();

        log.info("Found {} pending orders", orders.size());

        return ResponseEntity.ok(orders);
    }

    // MISE À JOUR DE COMMANDE

    /**
     * Updates the status of the specified order. The status of the order can be
     * changed to one of the following: CREATED, CONFIRMED, CANCELLED, or DELIVERED.
     * Requires ADMIN role for access.
     *
     * @param orderId the unique identifier of the order
     * @param status the new status to be applied to the order
     * @return a ResponseEntity containing a SuccessResponseDto which indicates
     *         whether the status update was successful
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Mettre à jour le statut d'une commande",
            description = "Change le statut d'une commande (CREATED, CONFIRMED, CANCELLED, DELIVERED)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Statut mis à jour avec succès",
                    content = @Content(schema = @Schema(implementation = SuccessResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Statut invalide ou transition non autorisée"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Commande non trouvée"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (role ADMIN requis)"
            )
    })
    public ResponseEntity<SuccessResponseDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status
    ) {
        log.info("Updating order {} status to: {}", orderId, status);

        orderService.updateOrderStatus(orderId, status);

        log.info("Order {} status updated successfully", orderId);

        return ResponseEntity.ok(
                SuccessResponseDto.builder()
                        .success(true)
                        .message("Statut de la commande mis à jour avec succès")
                        .build()
        );
    }
}