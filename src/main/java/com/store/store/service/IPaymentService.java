package com.store.store.service;

import com.store.store.dto.PaymentIntentRequestDto;
import com.store.store.dto.PaymentIntentResponseDto;

/**
 * Interface for managing payment-related operations.
 * Provides a method to create a payment intent, typically by interacting
 * with a payment gateway like Stripe.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
public interface IPaymentService {

    PaymentIntentResponseDto createPaymentIntent(PaymentIntentRequestDto requestDto);
}
