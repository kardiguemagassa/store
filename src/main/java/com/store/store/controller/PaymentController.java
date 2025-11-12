package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.payment.PaymentIntentRequestDto;
import com.store.store.dto.payment.PaymentIntentResponseDto;
import com.store.store.exception.BusinessException;
import com.store.store.service.IPaymentService;

import com.store.store.service.impl.MessageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-01
 *
 * @see IPaymentService
 * @see PaymentIntentRequestDto
 * @see PaymentIntentResponseDto
 */
@Tag(name = "Payment", description = "API de gestion des paiements Stripe")
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final IPaymentService paymentService;
    private final MessageServiceImpl messageService;

    @Operation(
            summary = "Créer un Payment Intent Stripe",
            description = "Initialise un paiement en créant un Payment Intent Stripe. " +
                    "Retourne un client secret à utiliser avec Stripe Elements pour confirmer le paiement."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment Intent créé avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données invalides (montant, devise)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Erreur Stripe (carte refusée, limite dépassée, etc.)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Erreur serveur (API Stripe indisponible)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PostMapping("/create-payment-intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponseDto>> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequestDto paymentRequest) {

        log.info("POST /api/v1/payment/create-payment-intent - amount: {} {}",
                paymentRequest.amount(), paymentRequest.currency());

        // Appel au service (retourne PaymentIntentResponseDto)
        PaymentIntentResponseDto paymentResponse = paymentService.createPaymentIntent(paymentRequest);

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.payment.intent.created",
                paymentResponse.paymentIntentId()
        );

        // Wrapper dans ApiResponse
        ApiResponse<PaymentIntentResponseDto> response = ApiResponse.success(successMessage, paymentResponse)
                .withPath("/api/v1/payment/create-payment-intent");

        log.info("Payment Intent created successfully: {} (amount: {} {})",
                paymentResponse.paymentIntentId(),
                paymentResponse.amount(),
                paymentResponse.currency());

        return ResponseEntity.ok(response);
    }
}