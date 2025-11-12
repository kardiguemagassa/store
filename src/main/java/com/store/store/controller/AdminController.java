package com.store.store.controller;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.contact.ContactResponseDto;
import com.store.store.dto.order.OrderResponseDto;
import com.store.store.dto.user.CustomerWithRolesDto;
import com.store.store.dto.user.PromotionResponseDto;
import com.store.store.entity.Customer;
import com.store.store.enums.RoleType;

import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.CustomerRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.IContactService;
import com.store.store.service.IOrderService;
import com.store.store.service.IRoleAssignmentService;

import com.store.store.service.impl.MessageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-06
 */
@Tag(name = "Admin", description = "API d'administration (accès ADMIN uniquement)")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminController {

    private final IOrderService orderService;
    private final IContactService contactService;
    private final ExceptionFactory exceptionFactory;
    private final CustomerRepository customerRepository;
    private final IRoleAssignmentService roleAssignmentService;
    private final MessageServiceImpl messageService;

    // GESTION DES UTILISATEURS - ATTRIBUTION DE RÔLES
    @Operation(
            summary = "Attribuer un rôle à un utilisateur",
            description = "Ajoute un rôle spécifique (USER, EMPLOYEE, MANAGER, ADMIN) à un utilisateur. " +
                    "Un utilisateur peut avoir plusieurs rôles simultanément."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rôle attribué avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Utilisateur non trouvé"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (rôle ADMIN requis)"
            )
    })
    @PostMapping("/users/{customerId}/roles/{roleType}")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @Parameter(description = "ID de l'utilisateur", required = true)
            @PathVariable Long customerId,

            @Parameter(description = "Type de rôle", required = true)
            @PathVariable RoleType roleType,

            @AuthenticationPrincipal CustomerUserDetails admin) {

        log.info("POST /api/v1/admin/users/{}/roles/{} - Admin: {}",
                customerId, roleType, admin.getUsername());

        // Appel au service
        roleAssignmentService.assignRole(customerId, roleType);

        log.info("Admin {} assigned role {} to user {}",
                admin.getUsername(), roleType, customerId);

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.role.assigned",
                roleType.getDisplayName(),
                customerId
        );

        ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/admin/users/" + customerId + "/roles/" + roleType);

        return ResponseEntity.ok(response);
    }


    // GESTION DES UTILISATEURS - PROMOTION
    @Operation(
            summary = "Promouvoir un utilisateur au niveau supérieur",
            description = "Fait progresser l'utilisateur dans la hiérarchie : USER → EMPLOYEE → MANAGER → ADMIN"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Promotion réussie",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Utilisateur déjà au niveau maximum (ADMIN)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Utilisateur non trouvé"
            )
    })
    @PostMapping("/users/{customerId}/promote")
    public ResponseEntity<ApiResponse<PromotionResponseDto>> promoteUser(
            @Parameter(description = "ID de l'utilisateur à promouvoir", required = true)
            @PathVariable Long customerId,

            @AuthenticationPrincipal CustomerUserDetails admin) {

        log.info("POST /api/v1/admin/users/{}/promote - Admin: {}",
                customerId, admin.getUsername());

        // Récupération de l'utilisateur
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "User", "id", customerId.toString()
                ));

        // Détermination du prochain rôle
        RoleType nextRole = determineNextRole(customer);

        // Attribution du nouveau rôle
        roleAssignmentService.assignRole(customerId, nextRole);

        // Construction de la réponse
        PromotionResponseDto promotionData = PromotionResponseDto.fromPromotion(
                customer,
                nextRole,
                admin.getUsername()
        );

        log.info("User {} promoted to {} by {}",
                customerId, nextRole, admin.getUsername());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.user.promoted",
                nextRole.getDisplayName()
        );

        ApiResponse<PromotionResponseDto> response = ApiResponse.success(successMessage, promotionData)
                .withPath("/api/v1/admin/users/" + customerId + "/promote");

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Promouvoir directement au rôle ADMIN",
            description = "Accorde immédiatement les privilèges administrateur complets. " +
                    "Action sensible, à utiliser avec précaution."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Promotion ADMIN réussie"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Utilisateur non trouvé"
            )
    })
    @PostMapping("/users/{userId}/promote-to-admin")
    public ResponseEntity<ApiResponse<PromotionResponseDto>> promoteToAdmin(
            @Parameter(description = "ID de l'utilisateur", required = true)
            @PathVariable @Min(1) Long userId,

            @AuthenticationPrincipal CustomerUserDetails adminUser) {

        log.info("POST /api/v1/admin/users/{}/promote-to-admin - Initiated by: {}",
                userId, adminUser.getUsername());

        // Promotion au rôle ADMIN
        roleAssignmentService.promoteToAdmin(userId, adminUser.getUsername());

        // Récupération de l'utilisateur promu
        Customer promotedUser = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "User", "id", userId.toString()
                ));

        // Construction de la réponse
        PromotionResponseDto promotionData = PromotionResponseDto.fromPromotion(
                promotedUser,
                RoleType.ROLE_ADMIN,
                adminUser.getUsername()
        );

        log.info("User {} promoted to ADMIN by {}", userId, adminUser.getUsername());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.user.promoted.admin"
        );

        ApiResponse<PromotionResponseDto> response = ApiResponse.success(successMessage, promotionData)
                .withPath("/api/v1/admin/users/" + userId + "/promote-to-admin");

        return ResponseEntity.ok(response);
    }

    // GESTION DES UTILISATEURS - RÉTROGRADATION
    @Operation(
            summary = "Rétrograder un ADMIN en utilisateur normal",
            description = "Retire les privilèges administrateur d'un utilisateur. " +
                    "Les autres rôles (MANAGER, EMPLOYEE, USER) sont conservés."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rétrogradation réussie"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Utilisateur non trouvé"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Utilisateur n'est pas administrateur"
            )
    })
    @PostMapping("/users/{userId}/demote-from-admin")
    public ResponseEntity<ApiResponse<PromotionResponseDto>> demoteFromAdmin(
            @Parameter(description = "ID de l'administrateur", required = true)
            @PathVariable @Min(1) Long userId,

            @AuthenticationPrincipal CustomerUserDetails adminUser) {

        log.info("POST /api/v1/admin/users/{}/demote-from-admin - Initiated by: {}",
                userId, adminUser.getUsername());

        // Rétrogradation
        roleAssignmentService.demoteFromAdmin(userId, adminUser.getUsername());

        // Récupération de l'utilisateur rétrogradé
        Customer demotedUser = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "User", "id", userId.toString()
                ));

        // Construction de la réponse
        PromotionResponseDto demotionData = PromotionResponseDto.fromPromotion(
                demotedUser,
                RoleType.ROLE_ADMIN,  // Rôle retiré
                adminUser.getUsername()
        );

        log.info("Admin privileges removed from user {} by {}", userId, adminUser.getUsername());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.user.demoted"
        );

        ApiResponse<PromotionResponseDto> response = ApiResponse.success(successMessage, demotionData)
                .withPath("/api/v1/admin/users/" + userId + "/demote-from-admin");

        return ResponseEntity.ok(response);
    }

    // GESTION DES UTILISATEURS - RETRAIT DE RÔLE
    @Operation(
            summary = "Retirer un rôle à un utilisateur",
            description = "Retire un rôle spécifique. L'utilisateur conserve au minimum ROLE_USER."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rôle retiré avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Utilisateur non trouvé"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Impossible de retirer le dernier rôle"
            )
    })
    @DeleteMapping("/users/{customerId}/roles/{roleType}")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @Parameter(description = "ID de l'utilisateur", required = true)
            @PathVariable Long customerId,

            @Parameter(description = "Rôle à retirer", required = true)
            @PathVariable RoleType roleType) {

        log.info("DELETE /api/v1/admin/users/{}/roles/{}", customerId, roleType);

        // Appel au service
        roleAssignmentService.removeRole(customerId, roleType);

        log.info("Role {} removed from user {}", roleType, customerId);

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.role.removed",
                roleType.getDisplayName()
        );

        ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/admin/users/" + customerId + "/roles/" + roleType);

        return ResponseEntity.ok(response);
    }

    // GESTION DES UTILISATEURS - CONSULTATION
    @Operation(
            summary = "Lister tous les utilisateurs avec leurs rôles",
            description = "Retourne une liste paginée de tous les utilisateurs du système avec leurs rôles assignés"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste récupérée avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<CustomerWithRolesDto>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("GET /api/v1/admin/users - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        // Appel au service
        Page<CustomerWithRolesDto> users = roleAssignmentService.getAllCustomersWithRoles(pageable);

        log.info("Retrieved {} users (page {}/{})",
                users.getNumberOfElements(),
                pageable.getPageNumber() + 1,
                users.getTotalPages());

        // Message de succès localisé avec détails
        String successMessage = messageService.getMessage(
                "api.success.admin.users.retrieved",
                users.getTotalElements(),
                pageable.getPageNumber() + 1,
                users.getTotalPages()
        );

        ApiResponse<Page<CustomerWithRolesDto>> response = ApiResponse.success(successMessage, users)
                .withPath("/api/v1/admin/users");

        return ResponseEntity.ok(response);
    }


    // DÉTERMINER PROCHAIN RÔLE
    private RoleType determineNextRole(Customer customer) {
        if (customer.isAdmin()) {
            // Utilisation de messageService
            throw exceptionFactory.businessError(
                    messageService.getMessage("error.admin.user.already.max.role")
            );
        }
        if (customer.isManager()) {
            return RoleType.ROLE_ADMIN;
        }
        if (customer.isEmployee()) {
            return RoleType.ROLE_MANAGER;
        }
        // Par défaut, USER → EMPLOYEE
        return RoleType.ROLE_EMPLOYEE;
    }


    // GESTION DES COMMANDES
    @Operation(
            summary = "Obtenir toutes les commandes en attente",
            description = "Retourne la liste des commandes avec statut CREATED nécessitant une action administrative"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des commandes en attente",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getAllPendingOrders() {
        log.info("GET /api/v1/admin/orders - Fetching pending orders");

        // Appel au service
        List<OrderResponseDto> orders = orderService.getAllPendingOrders();

        log.info("Found {} pending orders", orders.size());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.orders.pending.retrieved",
                orders.size()
        );

        ApiResponse<List<OrderResponseDto>> response = ApiResponse.success(successMessage, orders)
                .withPath("/api/v1/admin/orders");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Confirmer une commande",
            description = "Change le statut de la commande vers CONFIRMED pour démarrer la préparation"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Commande confirmée avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Commande non trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Transition de statut non autorisée"
            )
    })
    @PatchMapping("/orders/{orderId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(
            @Parameter(description = "ID de la commande", required = true)
            @PathVariable @Positive(message = "L'ID de commande doit être positif") Long orderId) {

        log.info("PATCH /api/v1/admin/orders/{}/confirm", orderId);

        // Appel au service
        orderService.updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);

        log.info("Order {} confirmed successfully", orderId);

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.order.confirmed",
                orderId
        );

        ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/admin/orders/" + orderId + "/confirm");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Annuler une commande",
            description = "Change le statut vers CANCELLED (action irréversible). " +
                    "Déclenche un remboursement automatique."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Commande annulée avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Commande non trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Impossible d'annuler (déjà livrée ou annulée)"
            )
    })
    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @Parameter(description = "ID de la commande", required = true)
            @PathVariable @Positive(message = "L'ID de commande doit être positif") Long orderId) {

        log.info("PATCH /api/v1/admin/orders/{}/cancel", orderId);

        // Appel au service
        orderService.updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);

        log.info("Order {} cancelled successfully", orderId);

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.order.cancelled",
                orderId
        );

        ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/admin/orders/" + orderId + "/cancel");

        return ResponseEntity.ok(response);
    }

    // GESTION DES MESSAGES DE CONTACT
    @Operation(
            summary = "Obtenir tous les messages ouverts",
            description = "Retourne la liste des messages de contact avec statut OPEN nécessitant un traitement"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des messages ouverts",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ContactResponseDto>>> getAllOpenMessages() {
        log.info("GET /api/v1/admin/messages - Fetching open messages");

        // Appel au service
        List<ContactResponseDto> messages = contactService.getAllOpenMessages();

        log.info("Found {} open messages", messages.size());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.messages.open.retrieved",
                messages.size()
        );

        ApiResponse<List<ContactResponseDto>> response = ApiResponse.success(successMessage, messages)
                .withPath("/api/v1/admin/messages");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Fermer un message de contact",
            description = "Change le statut du message vers CLOSED après traitement"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Message clôturé avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Message non trouvé"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Message déjà clôturé"
            )
    })
    @PatchMapping("/messages/{contactId}/close")
    public ResponseEntity<ApiResponse<Void>> closeMessage(
            @Parameter(description = "ID du message", required = true)
            @PathVariable @Positive(message = "L'ID de contact doit être positif") Long contactId) {

        log.info("PATCH /api/v1/admin/messages/{}/close", contactId);

        // Appel au service
        contactService.updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);

        log.info("Contact message {} closed successfully", contactId);

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.admin.message.closed",
                contactId
        );

        ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/admin/messages/" + contactId + "/close");

        return ResponseEntity.ok(response);
    }
}