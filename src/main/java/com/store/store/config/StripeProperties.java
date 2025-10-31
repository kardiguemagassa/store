package com.store.store.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;

/**
 * Propriétés de configuration Stripe.
 *
 * ✅ SOLUTION 3 CORRIGÉE : Validation manuelle dans @PostConstruct
 * - Séparation des responsabilités (SRP)
 * - Validation manuelle pour éviter les problèmes de chargement
 * - Facilite les tests unitaires
 * - Meilleure organisation pour grandes applications
 *
 * @author Kardigué
 * @version 1.1 - FIXED
 * @since 2025-01-27
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /**
     * Clé API Stripe (obligatoire).
     * Format: sk_test_... ou sk_live_...
     * La validation est faite manuellement dans @PostConstruct
     */
    @NotBlank(message = "Stripe API Key is required. Set STRIPE_API_KEY environment variable.")
    private String apiKey;

    /**
     * Secret webhook Stripe (optionnel).
     * Format: whsec_...
     */
    private String webhookSecret;

    /**
     * Clé publique Stripe (optionnel).
     * Utilisée côté client.
     */
    private String publishableKey;

    /**
     * Timeout pour les requêtes Stripe en secondes.
     */
    private Integer timeoutSeconds = 30;

    /**
     * Nombre maximum de tentatives en cas d'erreur.
     */
    private Integer maxRetries = 3;

    /**
     * Activer le mode test.
     */
    private boolean testMode = true;

    /**
     * Validation manuelle après le chargement des propriétés.
     * Permet d'avoir des messages d'erreur plus clairs.
     */
    @PostConstruct
    public void validateAndLog() {
        log.info("========================================");
        log.info("STRIPE CONFIGURATION");
        log.info("========================================");

        // Validation de la clé API
        validateApiKey();

        // Validation du webhook secret (si fourni)
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            validateWebhookSecret();
        }

        // Log de la configuration
        logConfiguration();

        log.info("========================================");
    }

    /**
     * Valide que la clé API Stripe est correcte.
     */
    private void validateApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("❌ Stripe API Key is not configured!");
            log.error("💡 Set STRIPE_API_KEY environment variable");
            throw new IllegalStateException(
                    "Stripe API Key is required. Set STRIPE_API_KEY environment variable."
            );
        }

        // Vérifier que ce n'est pas un placeholder non résolu
        if (apiKey.startsWith("${") && apiKey.endsWith("}")) {
            log.error("❌ Stripe API Key is not resolved: {}", apiKey);
            log.error("💡 Make sure STRIPE_API_KEY environment variable is set");
            log.error("💡 In IntelliJ: Run → Edit Configurations → EnvFile → Enable EnvFile");
            throw new IllegalStateException(
                    "Stripe API Key placeholder not resolved. Environment variable not loaded."
            );
        }

        // Vérifier le format
        if (!apiKey.startsWith("sk_test_") && !apiKey.startsWith("sk_live_")) {
            log.error("❌ Invalid Stripe API Key format: {}", maskApiKey(apiKey));
            log.error("💡 Key must start with 'sk_test_' or 'sk_live_'");
            throw new IllegalStateException(
                    "Invalid Stripe API Key format. Must start with 'sk_test_' or 'sk_live_'"
            );
        }

        // Déterminer le mode (test ou live)
        testMode = apiKey.startsWith("sk_test_");

        log.info("✅ Stripe API Key validated");
        log.info("🔑 API Key: {}", maskApiKey(apiKey));
        log.info("🧪 Mode: {}", testMode ? "TEST" : "LIVE");
    }

    /**
     * Valide le webhook secret (si fourni).
     */
    private void validateWebhookSecret() {
        if (!webhookSecret.startsWith("whsec_")) {
            log.warn("⚠️ Invalid Webhook Secret format. Should start with 'whsec_'");
            log.warn("💡 Webhook validation may not work correctly");
        } else {
            log.info("✅ Webhook Secret configured");
        }
    }

    /**
     * Log la configuration Stripe.
     */
    private void logConfiguration() {
        log.info("⏱️  Timeout: {} seconds", timeoutSeconds);
        log.info("🔄 Max Retries: {}", maxRetries);

        if (publishableKey != null && !publishableKey.isEmpty()) {
            log.info("✅ Publishable Key: {}", maskApiKey(publishableKey));
        } else {
            log.info("⚠️ Publishable Key: NOT CONFIGURED");
        }

        // Log des variables d'environnement pour debug
        logEnvironmentVariables();
    }

    /**
     * Log les variables d'environnement chargées (pour debug).
     */
    private void logEnvironmentVariables() {
        log.debug("========================================");
        log.debug("ENVIRONMENT VARIABLES (DEBUG)");
        log.debug("========================================");
        log.debug("DATABASE_HOST: {}", System.getenv("DATABASE_HOST"));
        log.debug("DATABASE_NAME: {}", System.getenv("DATABASE_NAME"));
        log.debug("SPRING_PROFILE: {}", System.getenv("SPRING_PROFILE"));
        log.debug("JWT_SECRET: {}", System.getenv("JWT_SECRET") != null ? "✅ SET" : "❌ NOT SET");
        log.debug("========================================");
    }

    /**
     * Masque la clé API pour les logs (sécurité).
     *
     * Exemple:
     * - Input:  sk_test_51H1234567890abcdefghijklmnopqrstuvwxyz
     * - Output: sk_test_51...wxyz
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 14) {
            return "INVALID_KEY";
        }
        return key.substring(0, 10) + "..." + key.substring(key.length() - 4);
    }

    /**
     * Vérifie si Stripe est en mode test.
     */
    public boolean isTestMode() {
        return testMode;
    }

    /**
     * Vérifie si Stripe est correctement configuré.
     */
    public boolean isConfigured() {
        return apiKey != null &&
                !apiKey.isEmpty() &&
                !apiKey.startsWith("${") &&
                (apiKey.startsWith("sk_test_") || apiKey.startsWith("sk_live_"));
    }
}