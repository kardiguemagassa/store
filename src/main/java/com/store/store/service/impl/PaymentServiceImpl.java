package com.store.store.service.impl;

import com.store.store.dto.PaymentIntentRequestDto;
import com.store.store.dto.PaymentIntentResponseDto;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.service.IPaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

    @Override
    public PaymentIntentResponseDto createPaymentIntent(PaymentIntentRequestDto requestDto) {
        try {
            log.info("Creating payment intent for amount: {} {}", requestDto.amount(), requestDto.currency());

            // Validation des données d'entrée
            validatePaymentRequest(requestDto);

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(requestDto.amount())
                    .setCurrency(requestDto.currency().toLowerCase()) // Stripe nécessite lowercase
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Vérification du client secret
            String clientSecret = paymentIntent.getClientSecret();
            if (clientSecret == null || clientSecret.isBlank()) {
                log.error("Stripe returned empty client secret for payment intent: {}", paymentIntent.getId());
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.payment.invalid.client.secret")
                );
            }

            log.info("Payment intent created successfully: {}", paymentIntent.getId());
            return new PaymentIntentResponseDto(clientSecret);

        } catch (StripeException e) {
            log.error("Stripe API error while creating payment intent: {}", e.getMessage(), e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.payment.stripe.failed", e.getMessage())
            );

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while creating payment intent", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.payment.create")
            );
        }
    }

    // MÉTHODES DE VALIDATION MÉTIER
    private void validatePaymentRequest(PaymentIntentRequestDto requestDto) {
        if (requestDto == null) {
            throw exceptionFactory.validationError("requestDto",
                    getLocalizedMessage("validation.payment.request.required"));
        }

        if (requestDto.amount() == null || requestDto.amount() <= 0) {
            throw exceptionFactory.validationError("amount",
                    getLocalizedMessage("validation.payment.amount.invalid"));
        }

        if (requestDto.currency() == null || requestDto.currency().trim().isEmpty()) {
            throw exceptionFactory.validationError("currency",
                    getLocalizedMessage("validation.payment.currency.required"));
        }

        // Validation spécifique du format de devise
        if (!isValidCurrency(requestDto.currency())) {
            throw exceptionFactory.validationError("currency",
                    getLocalizedMessage("validation.payment.currency.invalid", requestDto.currency()));
        }

        // Validation du montant minimum (50 centimes pour Stripe)
        if (requestDto.amount() < 50) {
            throw exceptionFactory.validationError("amount",
                    getLocalizedMessage("validation.payment.amount.minimum", 50));
        }
    }

    private boolean isValidCurrency(String currency) {
        // Validation basique du format de devise (3 lettres)
        return currency != null &&
                currency.length() == 3 &&
                currency.matches("[A-Za-z]{3}");
    }

    // MÉTHODE UTILITAIRE POUR L'INTERNATIONALISATION
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}