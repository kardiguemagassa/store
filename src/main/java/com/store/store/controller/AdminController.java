package com.store.store.controller;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.ContactResponseDto;
import com.store.store.dto.OrderResponseDto;
import com.store.store.dto.PromotionResponseDto;
import com.store.store.dto.ResponseDto;
import com.store.store.entity.Customer;
import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.CustomerRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.IContactService;
import com.store.store.service.IOrderService;
import com.store.store.service.RoleAssignmentService;

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

    @PostMapping("/{userId}/promote-to-admin")
    public ResponseEntity<PromotionResponseDto> promoteToAdmin(
            @PathVariable @Min(1) Long userId,
            @AuthenticationPrincipal CustomerUserDetails adminUser) {

        log.info("Promotion de l'utilisateur {} initiée par {}", userId, adminUser.getUsername());

        roleAssignmentService.promoteToAdmin(userId, adminUser.getUsername());

        // Récupérer l'utilisateur promu pour construire la réponse
        Customer promotedUser = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("Utilisateur", "id", userId.toString()));

        PromotionResponseDto response = PromotionResponseDto.from(promotedUser, adminUser.getUsername());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/demote-from-admin")
    public ResponseEntity<PromotionResponseDto> demoteFromAdmin(
            @PathVariable @Min(1) Long userId,
            @AuthenticationPrincipal CustomerUserDetails adminUser) {

        log.info("Retrait des privilèges ADMIN pour l'utilisateur {} initié par {}",
                userId, adminUser.getUsername());

        roleAssignmentService.demoteFromAdmin(userId, adminUser.getUsername());

        Customer demotedUser = customerRepository.findById(userId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("Utilisateur", "id", userId.toString()));

        PromotionResponseDto response = PromotionResponseDto.builder()
                .message("Privilèges ADMIN retirés avec succès")
                .userEmail(demotedUser.getEmail())
                .promotedBy(adminUser.getUsername())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponseDto>> getAllPendingOrders() {
        return ResponseEntity.ok().body(iOrderService.getAllPendingOrders());
    }

    @PatchMapping("/orders/{orderId}/confirm")
    public ResponseEntity<ResponseDto> confirmOrder(
            @PathVariable @Positive(message = "L'ID de commande doit être positif") Long orderId) {
        iOrderService.updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CONFIRMED);
        return ResponseEntity.ok(
                new ResponseDto("200", "Commande #" + orderId + " a été approuvé.")
        );
    }

    @PatchMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ResponseDto> cancelOrder(
            @PathVariable @Positive(message = "L'ID de commande doit être positif") Long orderId) {
        iOrderService.updateOrderStatus(orderId, ApplicationConstants.ORDER_STATUS_CANCELLED);
        return ResponseEntity.ok(
                new ResponseDto("200", "Commande #" + orderId + " a été annulé.")
        );
    }

    @GetMapping("/messages")
    public ResponseEntity<List<ContactResponseDto>> getAllOpenMessages() {
        return ResponseEntity.ok(iContactService.getAllOpenMessages());
    }

    @PatchMapping("/messages/{contactId}/close")
    public ResponseEntity<ResponseDto> closeMessage(
            @PathVariable @Positive(message = "L'ID de contact doit être positif") Long contactId) {
        iContactService.updateMessageStatus(contactId, ApplicationConstants.CLOSED_MESSAGE);
        return ResponseEntity.ok(
                new ResponseDto("200", "Contact #" + contactId + " a été fermé.")
        );
    }
}