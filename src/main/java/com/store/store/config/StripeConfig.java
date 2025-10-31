package com.store.store.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Stripe - Initialise la librairie Stripe.
 *
 * Utilise StripeProperties (ARCHITECTURE AVANCÉE)
 * - Séparation properties/configuration
 * - Injection par constructeur (testable)
 * - Validation automatique par StripeProperties
 *
 * @author Kardigué
 * @version 1.1 - FIXED
 * @since 2025-01-27
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    private final StripeProperties stripeProperties;

    /**
     * Initialise Stripe avec la clé API au démarrage de l'application.
     *
     * Note: La validation est faite dans StripeProperties.@PostConstruct
     * Ce code s'exécute APRÈS la validation.
     */
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("INITIALIZING STRIPE SDK");
        log.info("========================================");

        // Vérifier que les propriétés sont valides
        if (!stripeProperties.isConfigured()) {
            log.error("❌ Stripe is not properly configured");
            throw new IllegalStateException("Stripe configuration is invalid");
        }

        // Initialiser Stripe
        initializeStripe();

        log.info("✅ Stripe SDK initialized successfully");
        log.info("========================================");
    }

    /**
     * Initialise la librairie Stripe avec les propriétés configurées.
     */
    private void initializeStripe() {
        // Définir la clé API
        Stripe.apiKey = stripeProperties.getApiKey();

        // Configuration des timeouts
        if (stripeProperties.getTimeoutSeconds() != null) {
            int timeoutMs = stripeProperties.getTimeoutSeconds() * 1000;
            Stripe.setConnectTimeout(timeoutMs);
            Stripe.setReadTimeout(timeoutMs);
            log.info("⏱️  Stripe timeouts set to {} seconds", stripeProperties.getTimeoutSeconds());
        }

        // Configuration du nombre de tentatives
        if (stripeProperties.getMaxRetries() != null) {
            Stripe.setMaxNetworkRetries(stripeProperties.getMaxRetries());
            log.info("🔄 Stripe max retries set to {}", stripeProperties.getMaxRetries());
        }

        // Log du mode
        String mode = stripeProperties.isTestMode() ? "TEST" : "LIVE";
        log.info("🧪 Stripe mode: {}", mode);

        if (!stripeProperties.isTestMode()) {
            log.warn("⚠️⚠️⚠️ PRODUCTION MODE - LIVE TRANSACTIONS ENABLED ⚠️⚠️⚠️");
        }
    }

    /**
     * Vérifie si Stripe est correctement configuré.
     */
    public boolean isConfigured() {
        return stripeProperties.isConfigured();
    }

    /**
     * Vérifie si Stripe est en mode test.
     */
    public boolean isTestMode() {
        return stripeProperties.isTestMode();
    }

    /**
     * Getter pour récupérer les propriétés Stripe.
     */
    public StripeProperties getProperties() {
        return stripeProperties;
    }
}