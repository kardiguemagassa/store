package com.store.store.service.impl;

import com.store.store.dto.PaymentIntentRequestDto;
import com.store.store.dto.PaymentIntentResponseDto;
import com.store.store.service.IPaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements IPaymentService {

    @Override
    public PaymentIntentResponseDto createPaymentIntent(PaymentIntentRequestDto requestDto) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(requestDto.amount())
                    .setCurrency(requestDto.currency())
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Vérification du client secret
            String clientSecret = paymentIntent.getClientSecret();
            if (clientSecret == null || clientSecret.isBlank()) {
                throw new RuntimeException("Stripe n'a pas retourné de client secret valide");
            }

            return new PaymentIntentResponseDto(clientSecret);

        } catch (StripeException e) {
            throw new RuntimeException("Échec de paiement", e);
        }
    }
}