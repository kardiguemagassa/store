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
 * Contrôleur REST pour la gestion des commandes
 *
 * Endpoints :
 * - POST /orders/validate : Valide une commande avant création
 * - POST /orders : Crée une nouvelle commande
 * - GET /orders/customer : Liste des commandes du client
 * - GET /orders/pending : Liste des commandes en attente (ADMIN)
 * - PATCH /orders/{id}/status : Met à jour le statut (ADMIN)
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

    // =====================================================
    // VALIDATION PRE-CREATION
    // =====================================================

     /*
    Seulement ces 2 methods avant

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody  OrderRequestDto requestDto) {
        iOrderService.createOrder(requestDto);
        return ResponseEntity.ok("Order created successfully!");
    }



    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> loadCustomerOrders() {
        return ResponseEntity.ok(orderService.getCustomerOrders());
    }*/


    /**
     * ✅ Valide une commande avant création
     * Permet au frontend de vérifier la validité avant soumission
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
     * Crée une nouvelle commande après paiement réussi
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

    // =====================================================
    // CONSULTATION DES COMMANDES
    // =====================================================

    /**
     * Récupère les commandes du client connecté
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
     * Récupère toutes les commandes en attente (ADMIN uniquement)
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

    // =====================================================
    // MISE À JOUR DE COMMANDE
    // =====================================================

    /**
     * Met à jour le statut d'une commande (ADMIN uniquement)
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