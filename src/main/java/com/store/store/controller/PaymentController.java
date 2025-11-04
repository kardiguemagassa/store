package com.store.store.controller;

import com.store.store.dto.PaymentIntentRequestDto;
import com.store.store.dto.PaymentIntentResponseDto;
import com.store.store.service.IPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PaymentController is a REST controller that manages payment-related operations.
 * This class is responsible for handling payment feature requests and delegating
 * the processing to the service layer, specifically the IPaymentService interface.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-10-01
 */
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService iPaymentService;

    /**
     * Creates a payment intent based on the provided payment request details.
     * This endpoint interacts with the payment service to generate a client secret,
     * which can be used by the client to finalize the payment.
     *
     * @param paymentRequest the payment request details containing the amount and currency
     *                        required to create the payment intent
     * @return a ResponseEntity containing the payment intent response with the client secret
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<PaymentIntentResponseDto> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequestDto paymentRequest) {
        PaymentIntentResponseDto response =
                iPaymentService.createPaymentIntent(paymentRequest);
        return ResponseEntity.ok(response);
    }

}

