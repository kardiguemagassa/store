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

/**
 * Implementation of the {@link IPaymentService} interface providing functionality
 * to handle payment-related operations using the Stripe API.
 *
 * This service is responsible for creating payment intents and processing
 * related validations to ensure data integrity and proper formatting.
 * It leverages Stripe's SDK and provides clean abstractions for interacting
 * with payment workflows.
 *
 * Key Features:
 * - Creates payment intents with specified amount and currency.
 * - Validates payment request data to ensure correctness.
 * - Handles internationalization for error messages and validations.
 * - Incorporates exception handling for errors returned by Stripe API,
 *   business logic validations, or unexpected issues.
 *
 * Dependencies:
 * - ExceptionFactory: Handles the creation of structured exceptions for
 *   validation and business errors.
 * - MessageSource: Supports internationalization and localized error messages.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements IPaymentService {

    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

    /**
     * Creates a payment intent with the specified amount and currency by interacting with the Stripe API.
     *
     * @param requestDto the data transfer object containing the payment amount and currency
     *                   required for creating the payment intent. Must not be null and
     *                   must adhere to validation constraints on amount and currency.
     * @return a {@link PaymentIntentResponseDto} containing the client secret of the newly created
     *         payment intent. This is required to finalize the payment on the client side.
     * @throws BusinessException if validation of the request fails, if the client secret
     *                            returned by Stripe is invalid, or in case of unexpected errors.
     */
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

    /**
     * Validates the provided payment request data transfer object. Ensures that the request meets
     * all necessary validation constraints including amount and currency requirements. Throws
     *
     * @param requestDto the data transfer object containing payment details such as amount and
     *                   currency to be validated. Must not be null. The amount must be greater
     *                   than or equal to 50 cents, and the currency must be a valid ISO 4217
     *                   code. If validations fail, the method throws appropriate exception.
     */
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

    /**
     * Validates whether the given currency string is in the correct format.
     * The currency must be a non-null string consisting of exactly 3 alphabetic characters
     * (e.g., valid ISO 4217 currency codes).
     *
     * @param currency the currency string to validate. Must be a 3-letter string, non-null,
     *                 and match the pattern of three alphabetic characters (A-Z or a-z).
     * @return true if the currency string is valid; false otherwise.
     */
    private boolean isValidCurrency(String currency) {
        // Validation basique du format de devise (3 lettres)
        return currency != null &&
                currency.length() == 3 &&
                currency.matches("[A-Za-z]{3}");
    }

    // MÉTHODE UTILITAIRE POUR L'INTERNATIONALISATION

    /**
     * Retrieves a localized message based on the provided message code and arguments, using the current locale
     * in the LocaleContextHolder. This method delegates to the message source for resolving the message.
     *
     * @param code the message code to look up the localized message. Must not be null.
     * @param args optional array of arguments that can be used to replace placeholders in the message. Can be empty.
     * @return a localized message string corresponding to the provided code, formatted with the given arguments.
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}