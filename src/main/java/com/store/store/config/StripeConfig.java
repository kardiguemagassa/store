package com.store.store.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for managing Stripe SDK initialization and validation.
 *
 * This class initializes the Stripe SDK during application startup,
 * configures timeout settings, retry attempts, and API keys based on provided properties.
 * It also offers utility methods to check Stripe configuration and mode status.
 *
 * @author Kardigué
 * @version 1.1 - FIXED
 * @since 2025-10-27
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    private final StripeProperties stripeProperties;

    /**
     * Initializes the Stripe SDK upon application startup.
     *
     * This method is annotated with `@PostConstruct` to indicate that it should run
     * after the dependency injection is done and before the application is fully running.
     * It performs the following operations:
     *
     * 1. Logs the initialization process.
     * 2. Validates that the Stripe configuration properties are set correctly by
     *    calling `stripeProperties.isConfigured()`. If the configuration is invalid,
     *    an `IllegalStateException` is thrown.
     * 3. Calls the `initializeStripe` method to set up the Stripe SDK with the appropriate
     *    API key, timeout settings, retry configurations, and operational mode (test/live).
     *
     * If the Stripe configuration is invalid, an error message is logged, and
     * the application terminates with an exception.
     *
     * This ensures that the Stripe SDK is properly set up before handling any API calls.
     *
     * @throws IllegalStateException if the Stripe SDK configuration is invalid.
     */
    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("INITIALIZING STRIPE SDK");
        log.info("========================================");

        // Vérifier que les propriétés sont valides
        if (!stripeProperties.isConfigured()) {
            log.error("Stripe is not properly configured");
            throw new IllegalStateException("Stripe configuration is invalid");
        }

        // Initialiser Stripe
        initializeStripe();

        log.info("Stripe SDK initialized successfully");
        log.info("========================================");
    }

    /**
     * Configures the Stripe SDK with the appropriate initialization parameters.
     *
     * This method is responsible for setting up the Stripe SDK using configurations
     * provided by the `stripeProperties` object. The following actions are performed:
     *
     * 1. Sets the Stripe API key from the `stripeProperties`.
     * 2. Configures connection and read timeouts based on the `timeoutSeconds` value
     *    in `stripeProperties`. The timeout is applied only if the value is non-null.
     * 3. Configures the maximum number of network retries based on the `maxRetries`
     *    value in `stripeProperties`. This is applied only if the value is non-null.
     * 4. Logs the current operational mode of Stripe (TEST or LIVE) based on the
     *    `isTestMode` method in `stripeProperties`. If in LIVE mode, a warning
     *    message is logged to indicate that live transactions are enabled.
     *
     * It ensures that Stripe is properly configured before making any transaction
     * requests. The method also logs relevant configuration details for debugging
     * and operational monitoring.
     *
     * Note: This method is intended to be called during the application initialization
     * process, typically as part of a setup or configuration workflow.
     */
    private void initializeStripe() {
        // Définir la clé API
        Stripe.apiKey = stripeProperties.getApiKey();

        // Configuration des timeouts
        if (stripeProperties.getTimeoutSeconds() != null) {
            int timeoutMs = stripeProperties.getTimeoutSeconds() * 1000;
            Stripe.setConnectTimeout(timeoutMs);
            Stripe.setReadTimeout(timeoutMs);
            log.info("Stripe timeouts set to {} seconds", stripeProperties.getTimeoutSeconds());
        }

        // Configuration du nombre de tentatives
        if (stripeProperties.getMaxRetries() != null) {
            Stripe.setMaxNetworkRetries(stripeProperties.getMaxRetries());
            log.info("Stripe max retries set to {}", stripeProperties.getMaxRetries());
        }

        // Log du mode
        String mode = stripeProperties.isTestMode() ? "TEST" : "LIVE";
        log.info("Stripe mode: {}", mode);

        if (!stripeProperties.isTestMode()) {
            log.warn("PRODUCTION MODE - LIVE TRANSACTIONS ENABLED");
        }
    }

    /**
     * Checks whether the Stripe configuration is set up correctly.
     *
     * This method verifies if the required properties for the Stripe configuration,
     * such as the API key, have been properly initialized and are valid.
     *
     * @return true if the Stripe configuration is valid and ready for use, false otherwise.
     */
    public boolean isConfigured() {
        return stripeProperties.isConfigured();
    }

    /**
     * Determines whether the application is running in test mode for Stripe configuration.
     *
     * This method retrieves the test mode status from the Stripe properties,
     * allowing the application to differentiate between test and live environments.
     *
     * @return true if the application is currently set to test mode, false otherwise.
     */
    public boolean isTestMode() {
        return stripeProperties.isTestMode();
    }

    /**
     * Retrieves the StripeProperties object associated with the current configuration.
     *
     * @return the StripeProperties object containing configuration details for Stripe.
     */
    public StripeProperties getProperties() {
        return stripeProperties;
    }
}