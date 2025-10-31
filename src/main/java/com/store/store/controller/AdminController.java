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

    // =====================================================
    // GESTION DES UTILISATEURS
    // =====================================================

    // ✅ EXISTANT : Assigner n'importe quel rôle
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


    @DeleteMapping("/users/{customerId}/roles/{roleType}")
    public ResponseEntity<?> removeRole(@PathVariable Long customerId, @PathVariable RoleType roleType) {

        roleAssignmentService.removeRole(customerId, roleType);

        return ResponseEntity.ok(SuccessResponseDto.success(
                "Role " + roleType.getDisplayName() + " removed successfully"
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<Page<CustomerWithRolesDto>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {

        Page<CustomerWithRolesDto> users = roleAssignmentService.getAllCustomersWithRoles(pageable);
        return ResponseEntity.ok(users);
    }

    // Déterminer le prochain rôle dans la hiérarchie
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

    // =====================================================
    // GESTION DES COMMANDES
    // =====================================================

    @Operation(summary = "Obtenir toutes les commandes en attente")
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> getAllPendingOrders() {
        log.info("GET /api/v1/admin/orders/pending - Fetching pending orders");
        return ResponseEntity.ok(iOrderService.getAllPendingOrders());
    }

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

    // =====================================================
    // GESTION DES MESSAGES DE CONTACT
    // =====================================================
    @Operation(summary = "Obtenir tous les messages ouverts")
    @GetMapping("/messages")// open
    public ResponseEntity<List<ContactResponseDto>> getAllOpenMessages() {
        log.info("GET /api/v1/admin/messages/open - Fetching open messages");
        return ResponseEntity.ok(iContactService.getAllOpenMessages());
    }

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