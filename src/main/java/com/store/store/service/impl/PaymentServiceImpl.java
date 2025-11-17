package com.store.store.service.impl;

import com.store.store.dto.payment.PaymentIntentRequestDto;
import com.store.store.dto.payment.PaymentIntentResponseDto;
import com.store.store.exception.ExceptionFactory;
import com.store.store.service.IPaymentService;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-01
 */
@Service
@Validated
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("eur", "usd", "gbp", "cad");
    private static final long MINIMUM_AMOUNT = 50L; // 0.50€
    private static final long MAXIMUM_AMOUNT = 99999999L; // 999,999.99€


    @Override
    public PaymentIntentResponseDto createPaymentIntent(PaymentIntentRequestDto requestDto) {
        log.info("Creating payment intent for amount: {} {}", requestDto.amount(), requestDto.currency());

        try {
            // 1. Validation métier supplémentaire
            validateBusinessRules(requestDto);

            // 2. Construction des paramètres Stripe
            PaymentIntentCreateParams params = buildPaymentIntentParams(requestDto);

            // 3. Appel API Stripe
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // 4. Validation de la réponse Stripe
            validateStripeResponse(paymentIntent);

            log.info("Payment intent created successfully: {}", paymentIntent.getId());

            // 5. Construction de la réponse
            return buildResponseDto(paymentIntent);

        } catch (StripeException e) {
            handleStripeException(e);
            // Fallback (never reached car handleStripeException lance toujours une exception)
            throw exceptionFactory.businessError(messageService.getMessage("error.payment.stripe.failed"));
        } catch (Exception e) {
            log.error("Unexpected error while creating payment intent", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.internal.server"));
        }
    }

    // MÉTHODES DE VALIDATION MÉTIER

    private void validateBusinessRules(PaymentIntentRequestDto requestDto) {
        validateCurrencySupport(requestDto.currency());
        validateAmountRange(requestDto.amount());
        validateCurrencySpecificRules(requestDto.currency(), requestDto.amount());
    }

    private void validateCurrencySupport(String currency) {
        if (!SUPPORTED_CURRENCIES.contains(currency.toLowerCase())) {
            String errorMessage = messageService.getMessage("validation.payment.currency.unsupported", currency);
            throw exceptionFactory.validationError("currency", errorMessage);
        }
    }

    private void validateAmountRange(Long amount) {
        // Vérification montant minimum
        if (amount < MINIMUM_AMOUNT) {
            String errorMessage = messageService.getMessage("validation.payment.amount.minimum", MINIMUM_AMOUNT);
            throw exceptionFactory.validationError("amount", errorMessage);
        }
        // Vérification montant maximum
        if (amount > MAXIMUM_AMOUNT) {
            // Utilisation de messageService
            String errorMessage = messageService.getMessage("validation.payment.amount.maximum", MAXIMUM_AMOUNT);
            throw exceptionFactory.validationError("amount", errorMessage);
        }
    }

    private void validateCurrencySpecificRules(String currency, Long amount) {
        // Exemple: JPY (Yen japonais) ne supporte pas les décimales
        if ("jpy".equals(currency.toLowerCase()) && amount < 1) {
            // Utilisation de messageService
            String errorMessage = messageService.getMessage("validation.payment.amount.currency.min", currency, 1);
            throw exceptionFactory.validationError("amount", errorMessage);
        }
    }

    // MÉTHODES DE CONSTRUCTION

    private PaymentIntentCreateParams buildPaymentIntentParams(PaymentIntentRequestDto requestDto) {
        return PaymentIntentCreateParams.builder()
                .setAmount(requestDto.amount())
                .setCurrency(requestDto.currency().toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .putMetadata("application", "store-app")
                .putMetadata("environment", "production")
                .build();
    }

    private PaymentIntentResponseDto buildResponseDto(PaymentIntent paymentIntent) {
        return PaymentIntentResponseDto.fromStripeIntent(paymentIntent);
    }

    // MÉTHODES DE VALIDATION DE RÉPONSE

    private void validateStripeResponse(PaymentIntent paymentIntent) {
        // Vérification du client secret
        if (paymentIntent.getClientSecret() == null || paymentIntent.getClientSecret().isBlank()) {
            log.error("Stripe returned empty client secret for payment intent: {}", paymentIntent.getId());
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.payment.invalid.client.secret"));
        }

        // Vérification du statut
        if (paymentIntent.getStatus() == null) {
            log.error("Stripe returned null status for payment intent: {}", paymentIntent.getId());
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.payment.invalid.status"));
        }
    }

    // GESTION DES EXCEPTIONS

    private void handleStripeException(StripeException e) {
        log.error("Stripe API error while creating payment intent. Code: {}, Message: {}", e.getCode(), e.getMessage(), e);

        // Détermination du code d'erreur localisé
        String errorCode = switch (e.getCode()) {
            case "invalid_request_error" -> "error.payment.stripe.invalid_request";
            case "api_error" -> "error.payment.stripe.api_error";
            case "card_error" -> "error.payment.stripe.card_error";
            case "idempotency_error" -> "error.payment.stripe.idempotency_error";
            case "rate_limit_error" -> "error.payment.stripe.rate_limit";
            default -> "error.payment.stripe.failed";
        };

        // Récupération du message localisé
        String errorMessage = messageService.getMessage(errorCode);

        // Lance l'exception avec le message localisé
        throw exceptionFactory.businessError(errorMessage);
    }

    // MÉTHODES DE SUPPORT POUR LES TESTS

    protected long getMinimumAmount() {
        return MINIMUM_AMOUNT;
    }
    protected long getMaximumAmount() {
        return MAXIMUM_AMOUNT;
    }
    protected Set<String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }
}