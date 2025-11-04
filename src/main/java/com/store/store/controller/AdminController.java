package com.store.store.controller;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.*;
import com.store.store.entity.Customer;
import com.store.store.enums.RoleType;

import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.CustomerRepository;
import com.store.store.security.CustomerUserDetails;

import com.store.store.service.IContactService;
import com.store.store.service.IOrderService;
import com.store.store.service.IRoleAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller providing administrative API operations, accessible only to users
 * with the ADMIN role. Handles user management, order processing, and contact
 * message management.
 *
 * The controller enforces security restrictions ensuring only authorized
 * administrators can access the endpoints.
 *
 * Available Features:
 *
 * 1. User Management:
 *    - Assign roles to users.
 *    - Promote users to higher roles.
 *    - Demote ADMIN users to lower roles.
 *    - Remove roles from users.
 *    - Retrieve all users with their roles.
 *
 * 2. Order Management:
 *    - Retrieve all pending orders.
 *    - Confirm specific orders.
 *    - Cancel specific orders.
 *
 * 3. Contact Message Management:
 *    - Retrieve all open contact messages.
 *    - Close specific contact messages.
 *
 * The controller leverages services to handle core logic, ensure data integrity,
 * and manage the business rules related to administrative functionalities.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@Tag(name = "Admin", description = "API d'administration")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminController {

    private final IOrderService iOrderService;
    private final IContactService iContactService;
    private final ExceptionFactory exceptionFactory;
    private final CustomerRepository customerRepository;
    private final IRoleAssignmentService roleAssignmentService;


    // GESTION DES UTILISATEURS

    /**
     * Assigns a specific role to a user identified by their customer ID.
     *
     * @param customerId the ID of the customer to whom the role will be assigned
     * @param roleType the type of role to be assigned to the customer
     * @param admin the authenticated admin performing the operation
     * @return a ResponseEntity containing a SuccessResponseDto indicating the success of the role assignment
     */
    @PostMapping("/users/{customerId}/roles/{roleType}")
    @Operation(summary = "Assigner un rôle à un utilisateur")
    public ResponseEntity<SuccessResponseDto> assignRole(
            @PathVariable Long customerId,
            @PathVariable RoleType roleType,
            @AuthenticationPrincipal CustomerUserDetails admin) {

        roleAssignmentService.assignRole(customerId, roleType);

        log.info("Admin {} assigned role {} to user {}",
                admin.getUsername(), roleType, customerId);

        return ResponseEntity.ok(SuccessResponseDto.success(
                "Rôle " + roleType.getDisplayName() + " attribué avec succès"
        ));
    }

    /**
     * Promotes a user to the next role level in the hierarchy.
     *
     * @param customerId the ID of the customer to be promoted
     * @param admin the authenticated admin user performing the promotion
     * @return a ResponseEntity containing a PromotionResponseDto with details of the promotion
     */
    @PostMapping("/users/{customerId}/promote")
    @Operation(summary = "Promouvoir un utilisateur au niveau supérieur")
    public ResponseEntity<PromotionResponseDto> promoteUser(
            @PathVariable Long customerId,
            @AuthenticationPrincipal CustomerUserDetails admin) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "Utilisateur", "id", customerId.toString()
                ));

        RoleType nextRole = determineNextRole(customer);

        roleAssignmentService.assignRole(customerId, nextRole);

        PromotionResponseDto response = PromotionResponseDto.from(
                customer,
                nextRole,
                admin.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Promotes a user to the admin role.
     *
     * This method allows an authenticated admin user to promote another user
     * to the administrator role. It verifies the user's existence, updates their
     * role, and returns a response with the promotion details.
     *
     * @param userId the ID of the user to be promoted. Must be a positive integer.
     * @param adminUser the authenticated admin user performing the promotion.
     *
     * @return a ResponseEntity containing a {@link PromotionResponseDto} with
     *         the details of the promotion, including the promoted user's email,
     *         their new role, the admin who performed the promotion, and the timestamp.
     */
    @PostMapping("/users/{userId}/promote-to-admin")
    public ResponseEntity<PromotionResponseDto> promoteToAdmin(
            @PathVariable @Min(1) Long userId,
            @AuthenticationPrincipal CustomerUserDetails adminUser) {

        log.info("Promotion de l'utilisateur {} initiée par {}", userId, adminUser.getUsername());

        roleAssignmentService.promoteToAdmin(userId, adminUser.getUsername());

        Customer promotedUser = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "Utilisateur", "id", userId.toString()
                ));

        PromotionResponseDto response = PromotionResponseDto.from(
                promotedUser,
                RoleType.ROLE_ADMIN,
                adminUser.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Demotes an admin user to a regular user role.
     *
     * This method allows an authenticated admin to revoke the administrative privileges
     * of another user, identified by their user ID. It adjusts the user's roles accordingly
     * and returns a response containing the details of the demotion.
     *
     * @param userId the ID of the user to be demoted. Must be a positive integer.
     * @param adminUser the authenticated admin user performing the demotion.
     * @return a ResponseEntity containing a {@link PromotionResponseDto} with details
     *         of the demotion, including the user's email, the removed role, the admin
     *         who performed the action, and the timestamp.
     */
    @Operation(summary = "Rétrograder un ADMIN en utilisateur normal")
    @PostMapping("/users/{userId}/demote-from-admin")
    public ResponseEntity<PromotionResponseDto> demoteFromAdmin(
            @PathVariable @Min(1) Long userId,
            @AuthenticationPrincipal CustomerUserDetails adminUser) {

        log.info("Retrait des privilèges ADMIN pour l'utilisateur {} initié par {}",
                userId, adminUser.getUsername());

        roleAssignmentService.demoteFromAdmin(userId, adminUser.getUsername());

        Customer demotedUser = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "Utilisateur", "id", userId.toString()
                ));

        PromotionResponseDto response = PromotionResponseDto.demotion(
                demotedUser,
                RoleType.ROLE_ADMIN,  // Rôle retiré
                adminUser.getUsername()
        );

        return ResponseEntity.ok(response);
    }


    /**
     * Removes a specific role from a user identified by their customer ID.
     *
     * @param customerId the ID of the customer whose role is to be removed
     * @param roleType the type of role to be removed from the customer
     * @return a ResponseEntity containing a SuccessResponseDto indicating the success of the role removal operation
     */
    @DeleteMapping("/users/{customerId}/roles/{roleType}")
    public ResponseEntity<?> removeRole(@PathVariable Long customerId, @PathVariable RoleType roleType) {

        roleAssignmentService.removeRole(customerId, roleType);

        return ResponseEntity.ok(SuccessResponseDto.success(
                "Role " + roleType.getDisplayName() + " removed successfully"
        ));
    }

    /**
     * Retrieves a paginated list of all users along with their associated roles.
     * The users are represented as {@link CustomerWithRolesDto}.
     *
     * @param pageable the pagination information, including page size and page number
     * @return a {@code ResponseEntity} containing a {@code Page} object of {@code CustomerWithRolesDto},
     *         which represents the user data along with their roles
     */
    @GetMapping("/users")
    public ResponseEntity<Page<CustomerWithRolesDto>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {

        Page<CustomerWithRolesDto> users = roleAssignmentService.getAllCustomersWithRoles(pageable);
        return ResponseEntity.ok(users);
    }

    // Déterminer le prochain rôle dans la hiérarchie

    /**
     * Determines the next role to be assigned to a customer based on their current role.
     *
     * This method evaluates the current role of the provided customer and identifies the
     * next role in the role hierarchy. If the customer already possesses the highest role
     * (ADMIN), an exception is thrown.
     *
     * @param customer the customer whose role needs to be evaluated and promoted
     * @return the next {@code RoleType} in the hierarchy for the given customer
     * @throws BusinessException if the customer already has the highest role (ADMIN)
     */
    private RoleType determineNextRole(Customer customer) {
        if (customer.isAdmin()) {
            throw exceptionFactory.businessError("L'utilisateur a déjà le rôle le plus élevé");
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

    /**
     * Retrieves all pending orders.
     * This method interacts with the order service to fetch a list of orders
     * that are currently in a pending state, which is then returned
     * as a response entity.
     *
     * @return a ResponseEntity containing a list of OrderResponseDto objects
     *         representing the pending orders
     */
    @Operation(summary = "Obtenir toutes les commandes en attente")
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> getAllPendingOrders() {
        log.info("GET /api/v1/admin/orders/pending - Fetching pending orders");
        return ResponseEntity.ok(iOrderService.getAllPendingOrders());
    }

    /**
     * Confirms an order by updating its status to "ORDER_STATUS_CONFIRMED".
     *
     * This method processes a PATCH request to confirm the order corresponding
     * to the given order ID. Once confirmed, a response is returned indicating
     * the success of the operation.
     *
     * @param orderId the ID of the order to be confirmed. Must be a positive value.
     * @return a ResponseEntity containing a ResponseDto object with a status code
     *         and a message indicating the successful confirmation of the order.
     */
    @Operation(summary = "Confirmer une commande")
    @PatchMapping("/orders/{orderId}/confirm")
    public ResponseEntity<ResponseDto> confirmOrder(
            @PathVariable @Positive(message = "L'ID de commande doit être positif") Long orderId) {
        log.info("PATCH /api/v1/admin/orders/{}/confirm", orderId);
        iOrderService.updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
        return ResponseEntity.ok(
                new ResponseDto("200", "Commande #" + orderId + " a été approuvé.")
        );
    }

    /**
     * Cancels an order by updating its status to "ORDER_STATUS_CANCELLED".
     *
     * This method processes a PATCH request to cancel the order corresponding
     * to the given order ID. Once cancelled, a response is returned
     * indicating the success of the operation.
     *
     * @param orderId the ID of the order to be cancelled. Must be a positive value.
     * @return a ResponseEntity containing a ResponseDto object with a status code
     *         and a message indicating the successful cancellation of the order.
     */
    @Operation(summary = "Annuler une commande")
    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ResponseDto> cancelOrder(
            @PathVariable @Positive(message = "L'ID de commande doit être positif") Long orderId) {
        log.info("PATCH /api/v1/admin/orders/{}/cancel", orderId);
        iOrderService.updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);
        return ResponseEntity.ok(
                new ResponseDto("200", "Commande #" + orderId + " a été annulé.")
        );
    }


    // GESTION DES MESSAGES DE CONTACT

    /**
     * Retrieves all open messages from the system.
     *
     * @return a ResponseEntity containing a list of ContactResponseDto objects representing
     *         all messages that are marked as open.
     */
    @Operation(summary = "Obtenir tous les messages ouverts")
    @GetMapping("/messages")// open
    public ResponseEntity<List<ContactResponseDto>> getAllOpenMessages() {
        log.info("GET /api/v1/admin/messages/open - Fetching open messages");
        return ResponseEntity.ok(iContactService.getAllOpenMessages());
    }

    /**
     * Closes a contact message based on the provided contact ID and updates its status to closed.
     *
     * @param contactId the ID of the contact message to be closed; must be positive
     * @return a ResponseEntity containing a ResponseDto with the status code and a message indicating
     *         that the contact message was successfully closed
     */
    @Operation(summary = "Fermer un message de contact")
    @PatchMapping("/messages/{contactId}/close")
    public ResponseEntity<ResponseDto> closeMessage(
            @PathVariable @Positive(message = "L'ID de contact doit être positif") Long contactId) {
        log.info("PATCH /api/v1/admin/messages/{}/close", contactId);
        iContactService.updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);
        return ResponseEntity.ok(
                new ResponseDto("200", "Contact #" + contactId + " a été fermé.")
        );
    }
}