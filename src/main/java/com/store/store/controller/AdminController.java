package com.store.store.controller;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.*;
import com.store.store.entity.Customer;
import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.CustomerRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.ICategoryService;
import com.store.store.service.IContactService;
import com.store.store.service.IOrderService;
import com.store.store.service.RoleAssignmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RoleAssignmentService roleAssignmentService;

    // =====================================================
    // GESTION DES UTILISATEURS
    // =====================================================

    @Operation(summary = "Promouvoir un utilisateur en ADMIN")
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

        PromotionResponseDto response = PromotionResponseDto.from(promotedUser, adminUser.getUsername());

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

        PromotionResponseDto response = PromotionResponseDto.builder()
                .message("Privilèges ADMIN retirés avec succès")
                .userEmail(demotedUser.getEmail())
                .promotedBy(adminUser.getUsername())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
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