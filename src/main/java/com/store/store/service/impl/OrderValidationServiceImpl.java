package com.store.store.service.impl;

import com.store.store.dto.order.OrderItemDto;
import com.store.store.dto.order.OrderRequestDto;
import com.store.store.dto.order.OrderValidationResultDto;
import com.store.store.entity.Product;
import com.store.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderValidationServiceImpl {

    private final ProductRepository productRepository;
    private final MessageServiceImpl messageService;

    // VALIDATION PRINCIPALE

    public OrderValidationResultDto validateOrder(OrderRequestDto request) {
        log.debug("Starting order validation");

        List<String> errors = new ArrayList<>();

        // 1. Calculer le total attendu
        BigDecimal calculatedTotal = calculateTotalPrice(request);
        BigDecimal expectedTotal = request.getTotalPrice();

        log.debug("Total comparison - Calculated: {}, Expected: {}", calculatedTotal, expectedTotal);

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
            log.info("Order validation successful - Total: {}", calculatedTotal);
            return OrderValidationResultDto.valid(calculatedTotal, expectedTotal);
        } else {
            log.warn("Order validation failed with {} errors: {}", errors.size(), errors);
            return OrderValidationResultDto.invalid(errors, calculatedTotal, expectedTotal);
        }
    }

    // VALIDATION - CALCUL DU TOTAL

    private BigDecimal calculateTotalPrice(OrderRequestDto request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.debug("No items in order, returning 0.00");
            return BigDecimal.ZERO;
        }

        BigDecimal total = request.getItems().stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Calculated total from {} items: {}", request.getItems().size(), total);
        return total;
    }

    // VALIDATION - COHÉRENCE DU TOTAL

    private void validateTotalPrice(OrderRequestDto request, BigDecimal calculatedTotal, List<String> errors) {
        // Vérification 1 : Total présent
        if (request.getTotalPrice() == null) {
            errors.add(messageService.getMessage("validation.order.total.required"));
            return;
        }

        // Vérification 2 : Total positif
        if (request.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(messageService.getMessage("validation.order.total.positive"));
            return;
        }

        // Vérification 3 : Cohérence (tolérance 0.01€)
        BigDecimal difference = calculatedTotal.subtract(request.getTotalPrice()).abs();
        if (difference.compareTo(new BigDecimal("0.01")) > 0) {
            log.warn("Total price mismatch - Calculated: {}, Provided: {}, Difference: {}",
                    calculatedTotal, request.getTotalPrice(), difference);

            errors.add(messageService.getMessage("validation.order.total.mismatch", calculatedTotal, request.getTotalPrice()
            ));
        }
    }

    // VALIDATION - PRODUITS ET STOCK

    private void validateProducts(OrderRequestDto request, List<String> errors) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.debug("No items to validate");
            return;
        }

        log.debug("Validating {} products", request.getItems().size());

        for (OrderItemDto item : request.getItems()) {
            // Vérification 1 : Produit existe
            Product product = productRepository.findById(item.productId()).orElse(null);

            if (product == null) {
                log.warn("Product not found: ID {}", item.productId());
                errors.add(messageService.getMessage("validation.order.product.not.found", item.productId()
                ));
                continue; // Skip autres vérifications pour ce produit
            }

            // Vérification 2 : Stock suffisant
            // TODO : Remplacer popularity par un vrai champ stock
            int availableStock = product.getPopularity();
            if (availableStock < item.quantity()) {
                log.warn("Insufficient stock for product {} - Available: {}, Requested: {}",
                        product.getName(), availableStock, item.quantity());

                errors.add(messageService.getMessage("validation.order.insufficient.stock",
                        product.getName(),
                        availableStock,
                        item.quantity()
                ));
            }

            // Vérification 3 : Prix cohérent (détection fraude)
            // Tolérance : 50% du prix actuel
            BigDecimal priceDifference = product.getPrice().subtract(item.price()).abs();
            BigDecimal maxTolerance = product.getPrice().multiply(new BigDecimal("0.5"));

            if (priceDifference.compareTo(maxTolerance) > 0) {
                // Prix suspect mais pas bloquant (peut être promotion légitime)
                log.warn("Suspicious price for product {} ({}): DB price {}, Order price {} (difference {})",
                        product.getId(), product.getName(),
                        product.getPrice(), item.price(), priceDifference);
                // Note : Pas d'ajout d'erreur, juste un warning pour investigation
            }
        }
    }

    // VALIDATION - STATUT DE PAIEMENT

    private void validatePaymentStatus(OrderRequestDto request, List<String> errors) {
        // Vérification 1 : Statut présent
        if (request.getPaymentStatus() == null || request.getPaymentStatus().isBlank()) {
            errors.add(messageService.getMessage("validation.order.payment.status.required"));
            return;
        }

        // Normalisation du statut (Stripe/PayPal → format interne)
        String normalizedStatus = normalizePaymentStatus(request.getPaymentStatus());

        log.debug("Payment status - Original: '{}', Normalized: '{}'", request.getPaymentStatus(), normalizedStatus);

        // Vérification 2 : Paiement confirmé
        if (!"paid".equals(normalizedStatus) && !"succeeded".equals(request.getPaymentStatus())) {
            log.warn("Payment not confirmed - Status: '{}'", request.getPaymentStatus());
            errors.add(messageService.getMessage("validation.order.payment.not.confirmed", request.getPaymentStatus()));
        }
    }

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

    // VALIDATION - ARTICLES

    private void validateItems(OrderRequestDto request, List<String> errors) {
        // Vérification 1 : Liste présente
        if (request.getItems() == null || request.getItems().isEmpty()) {
            errors.add(messageService.getMessage("validation.order.items.required"));
            return;
        }

        // Calcul de la quantité totale
        int totalQuantity = request.getItems().stream().mapToInt(OrderItemDto::quantity).sum();

        log.debug("Total quantity across {} items: {}", request.getItems().size(), totalQuantity);

        // Vérification 2 : Quantité totale > 0
        if (totalQuantity == 0) {
            errors.add(messageService.getMessage("validation.order.items.empty"));
        }

        // Vérification 3 : Limite anti-abus (999 articles max)
        if (totalQuantity > 999) {
            log.warn("Order exceeds maximum quantity limit - Total: {}", totalQuantity);
            errors.add(messageService.getMessage("validation.order.items.too.many", totalQuantity));
        }
    }

}