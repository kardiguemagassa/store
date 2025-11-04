package com.store.store.service.impl;

import com.store.store.dto.OrderItemDto;
import com.store.store.dto.OrderRequestDto;
import com.store.store.dto.OrderValidationResultDto;
import com.store.store.entity.Product;
import com.store.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for order validation.
 *
 * This class is responsible for validating an order request based on various
 * business rules, including total price matching, product availability,
 * stock quantity, payment status, and item presence.
 *
 * It logs the validation process and returns a detailed result indicating
 * whether the order is valid and, if not, the errors encountered during validation.
 *
 * Dependencies:
 * - ProductRepository for verifying product details and stock.
 * - MessageSource for retrieving localized error messages.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderValidationServiceImpl {

    private final ProductRepository productRepository;
    private final MessageSource messageSource;

    /**
     * Validates the order request by performing checks on total price consistency, product availability,
     * payment status, and the presence of order items. Constructs a result indicating validation success
     * or failure along with calculated totals and error details if any.
     *
     * @param request the order request containing order details such as items, prices, and payment status
     * @return an {@code OrderValidationResultDto} object that encapsulates the validation status,
     *         error messages (if any), the calculated total price, and the expected total price
     */
    public OrderValidationResultDto validateOrder(OrderRequestDto request) {
        log.debug("Validating order request");

        List<String> errors = new ArrayList<>();

        // 1. Calculer le total attendu
        BigDecimal calculatedTotal = calculateTotalPrice(request);
        BigDecimal expectedTotal = request.getTotalPrice();

        // 2. Vérifier la cohérence du total
        validateTotalPrice(request, calculatedTotal, errors);

        // 3. Vérifier les produits et le stock
        validateProducts(request, errors);

        // 4. Vérifier le statut de paiement
        validatePaymentStatus(request, errors);

        // 5. Vérifier qu'il y a au moins un item
        validateItems(request, errors);

        // 6. Construire le résultat
        boolean isValid = errors.isEmpty();

        if (isValid) {
            log.debug("Order validation successful");
            return OrderValidationResultDto.valid(calculatedTotal, expectedTotal);
        } else {
            log.warn("Order validation failed with {} errors", errors.size());
            return OrderValidationResultDto.invalid(errors, calculatedTotal, expectedTotal);
        }
    }

    /**
     * Calculates the total price of all items in the order request by multiplying each item's price
     * by its quantity and summing the results.
     *
     * @param request the {@code OrderRequestDto} containing the list of items and their details
     * @return the total price of all items as a {@code BigDecimal}, or {@code BigDecimal.ZERO} if the item list is empty or null
     */
    private BigDecimal calculateTotalPrice(OrderRequestDto request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return request.getItems().stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates the consistency of the total price in the order request. Checks that the total price
     * is present, positive, and matches the calculated total price within an acceptable tolerance.
     *
     * @param request the order request containing the total price to be validated
     * @param calculatedTotal the total price calculated based on the individual items in the order
     * @param errors the list to store any validation error messages identified during the check
     */
    private void validateTotalPrice(
            OrderRequestDto request,
            BigDecimal calculatedTotal,
            List<String> errors
    ) {
        if (request.getTotalPrice() == null) {
            errors.add(getLocalizedMessage("validation.order.total.required"));
            return;
        }

        if (request.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(getLocalizedMessage("validation.order.total.positive"));
            return;
        }

        // Vérifier la cohérence (tolérance de 0.01 pour les arrondis)
        BigDecimal difference = calculatedTotal.subtract(request.getTotalPrice()).abs();
        if (difference.compareTo(new BigDecimal("0.01")) > 0) {
            errors.add(getLocalizedMessage(
                    "validation.order.total.mismatch",
                    calculatedTotal,
                    request.getTotalPrice()
            ));
        }
    }

    /**
     * Validates the list of products in the order request by checking their existence,
     * stock availability, and price consistency. Errors for any discrepancies are added
     * to the provided list of validation error messages.
     *
     * @param request the {@code OrderRequestDto} containing the list of order items
     *                to be validated
     * @param errors a {@code List} to store validation error messages identified
     *               during the validation process
     */
    private void validateProducts(OrderRequestDto request, List<String> errors) {
        if (request.getItems() == null) {
            return;
        }

        for (OrderItemDto item : request.getItems()) {
            // Vérifier que le produit existe
            Product product = productRepository.findById(item.productId()).orElse(null);

            if (product == null) {
                errors.add(getLocalizedMessage(
                        "validation.order.product.not.found",
                        item.productId()
                ));
                continue;
            }

            // Vérifier le stock (popularity utilisé comme stock temporairement)
            if (product.getPopularity() < item.quantity()) {
                errors.add(getLocalizedMessage(
                        "validation.order.insufficient.stock",
                        product.getName(),
                        product.getPopularity(),
                        item.quantity()
                ));
            }

            // Vérifier le prix (cohérence avec le prix actuel du produit)
            // Note: On peut tolérer des différences si des promotions sont appliquées
            BigDecimal priceDifference = product.getPrice().subtract(item.price()).abs();
            if (priceDifference.compareTo(product.getPrice().multiply(new BigDecimal("0.5"))) > 0) {
                // Si le prix diffère de plus de 50%, c'est suspect
                log.warn("Price mismatch for product {}: expected {}, got {}",
                        product.getId(), product.getPrice(), item.price());
            }
        }
    }

    /**
     * Validates the payment status within the provided order request.
     * Ensures the payment status is not null or empty, normalizes the
     * payment status, and checks that it confirms the payment has been
     * completed successfully. Any identified validation errors are added
     * to the provided error list.
     *
     * @param request the {@code OrderRequestDto} object containing the payment
     *                status and other order details to be validated.
     * @param errors  a {@code List} to store validation error messages
     *                identified during the validation process.
     */
    private void validatePaymentStatus(OrderRequestDto request, List<String> errors) {
        if (request.getPaymentStatus() == null || request.getPaymentStatus().isBlank()) {
            errors.add(getLocalizedMessage("validation.order.payment.status.required"));
            return;
        }

        // Normaliser le statut
        String normalizedStatus = normalizePaymentStatus(request.getPaymentStatus());

        // Vérifier que le paiement est confirmé
        if (!"paid".equals(normalizedStatus) && !"succeeded".equals(request.getPaymentStatus())) {
            errors.add(getLocalizedMessage(
                    "validation.order.payment.not.confirmed",
                    request.getPaymentStatus()
            ));
        }
    }

    /**
     * Validates the list of order items within the provided order request.
     * Ensures the item list is not null or empty, checks the total quantity of items,
     * and validates that the total quantity is within acceptable limits. If any validation
     * issues are found, corresponding error messages are added to the provided list.
     *
     * @param request the {@code OrderRequestDto} containing the list of order items
     *                to be validated
     * @param errors a {@code List} to store validation error messages identified
     *               during the validation process
     */
    private void validateItems(OrderRequestDto request, List<String> errors) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            errors.add(getLocalizedMessage("validation.order.items.required"));
            return;
        }

        // Vérifier la quantité totale
        int totalQuantity = request.getItems().stream()
                .mapToInt(OrderItemDto::quantity)
                .sum();

        if (totalQuantity == 0) {
            errors.add(getLocalizedMessage("validation.order.items.empty"));
        }

        if (totalQuantity > 999) {
            errors.add(getLocalizedMessage(
                    "validation.order.items.too.many",
                    totalQuantity
            ));
        }
    }

    /**
     * Normalizes the payment status by converting it to a standardized representation.
     * If the status is null, it defaults to "failed". Specific statuses are mapped to
     * predefined values ("paid", "pending", or "failed"), and any unrecognized status
     * is returned in lowercase.
     *
     * @param status the raw payment status as a string, which may be null or in any case format
     * @return a normalized payment status as a string: "paid", "pending", "failed", or the
     *         lowercase representation of an unrecognized status
     */
    private String normalizePaymentStatus(String status) {
        if (status == null) {
            return "failed";
        }

        return switch (status.toLowerCase()) {
            case "succeeded" -> "paid";
            case "processing", "requires_payment_method", "requires_confirmation", "requires_action" -> "pending";
            case "canceled", "failed" -> "failed";
            default -> status.toLowerCase();
        };
    }

    /**
     * Retrieves a localized message for the given code and arguments based on the current locale.
     *
     * @param code the code identifying the message to be retrieved
     * @param args the arguments that will be used to replace placeholders in the message
     * @return a localized message as a string, formatted with the provided arguments
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}