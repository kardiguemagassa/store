package com.store.store.service;

import com.store.store.dto.payment.PaymentIntentRequestDto;
import com.store.store.dto.payment.PaymentIntentResponseDto;

/**
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
public interface IPaymentService {

    PaymentIntentResponseDto createPaymentIntent(PaymentIntentRequestDto requestDto);
}
