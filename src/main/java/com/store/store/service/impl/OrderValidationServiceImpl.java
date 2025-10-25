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
 * Service de validation métier des commandes
 *
 * Responsabilités :
 * - Vérifier la cohérence des données de commande
 * - Valider le stock disponible
 * - Vérifier l'état du paiement
 * - Calculer et comparer les totaux
 *
 * Utilisé par :
 * - OrderController (endpoint /orders/validate)
 * - OrderServiceImpl (avant création de commande)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderValidationServiceImpl {

    private final ProductRepository productRepository;
    private final MessageSource messageSource;

    /**
     * Valide une requête de commande complète
     *
     * @param request La requête de commande à valider
     * @return Résultat de validation avec erreurs détaillées si invalide
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
     * Calcule le prix total basé sur les items
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
     * Valide la cohérence du total
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
     * Valide les produits et le stock
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
     * Valide le statut de paiement
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
     * Valide la présence d'items
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
     * Normalise le statut de paiement Stripe
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
     * Récupère un message localisé
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}