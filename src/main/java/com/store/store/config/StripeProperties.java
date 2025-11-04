package com.store.store.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties class for managing Stripe integration settings.
 *
 * This class provides settings required for integrating with the Stripe API,
 * including API keys, webhook secrets, timeout configurations, and other properties.
 *
 * The properties are loaded and managed via Spring's `@ConfigurationProperties` mechanism
 * with a prefix of `stripe`. This allows external configuration through application properties
 * or environment variables.
 *
 * @author Kardigué
 * @version 1.1 - FIXED
 * @since 2025-10-27
 */
@Data
@Slf4j
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    /**
     * Represents the secret API key for Stripe integration.
     *
     * This key is required for authenticating requests to the Stripe API. It must be
     * provided and set via the `STRIPE_API_KEY` environment variable. The value
     * cannot be blank or null.
     *
     * Validation is enforced to ensure the key is correctly configured before using
     * it for any Stripe operations.
     */
    @NotBlank(message = "Stripe API Key is required. Set STRIPE_API_KEY environment variable.")
    private String apiKey;

    /**
     * Represents the secret key used to validate incoming webhook requests from Stripe.
     * This secret is provided by Stripe when setting up webhooks and is used to ensure
     * that webhook payloads are authentic and originate from Stripe.
     *
     * It is recommended to keep this value securely stored and not expose it in logs
     * or insecure locations. The validation of this value ensures the integrity and
     * security of webhook events handled by the application.
     */
    private String webhookSecret;

    /**
     * The publishable key for authenticating client-side Stripe API requests.
     *
     * This key is safe to use in public environments, such as web or mobile clients,
     * and is used for operations like creating tokens or performing basic client-side
     * interactions with the Stripe API.
     */
    private String publishableKey;

    /**
     * Represents the timeout duration, in seconds, for operations related to Stripe configuration
     * or integration. This setting determines how long the application will wait for a response
     * before timing out. It is commonly used in network or API calls.
     *
     * Default value: 30 seconds.
     */
    private Integer timeoutSeconds = 30;

    /**
     * The maximum number of retry attempts to be made in case of a failure.
     *
     * This variable is used to define the upper limit for retrying operations
     * that encounter non-critical errors, such as network timeouts or API rate limits.
     * It plays a role in ensuring robust handling of transient issues while preventing
     * indefinite retries, which could lead to unnecessary overhead.
     *
     * Default value: 3
     */
    private Integer maxRetries = 3;

    /**
     * Indicates whether the application is running in test mode.
     * When set to true, the Stripe integration operates in a sandbox
     * environment, allowing for testing without real financial transactions.
     */
    private boolean testMode = true;

    /**
     * Validates and logs the configuration for Stripe integration.
     *
     * This method performs the following tasks:
     * - Validates the Stripe API Key to ensure it is properly configured and follows the correct format.
     * - Validates the Webhook Secret, if provided, ensuring it adheres to expected format conventions.
     * - Logs the Stripe configuration, including timeout settings, retry attempts, and other relevant details.
     *
     * Logs informative messages to indicate the success or warnings about the configuration.
     *
     * Throws:
     * - IllegalStateException if the API Key is invalid or not configured.
     *
     * Note: This method is annotated with `@PostConstruct` to be executed
     * automatically after the bean initialization phase in a Spring context.
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
     * Validates the configuration of the Stripe API Key.
     *
     * This method ensures that the `apiKey` field is properly configured, non-empty,
     * resolved, and adheres to expected format conventions for Stripe API keys.
     *
     * The validation process includes:
     * 1. Checking if the API Key is null or empty.
     *    - Logs error and throws an IllegalStateException if not configured.
     * 2. Verifying that the API Key is not an unresolved placeholder.
     *    - Logs error and throws an IllegalStateException if uninitialized.
     * 3. Ensuring the API Key format starts with either `sk_test_` or `sk_live_`.
     *    - Logs error and throws an IllegalStateException if the format is invalid.
     *
     * Additionally, this method determines the operational mode of the API Key
     * (either Test or Live mode) and logs the masked API Key for debugging.
     *
     * Logs informative messages for successful validation or configuration issues.
     *
     * Throws:
     * - IllegalStateException if the `apiKey` is invalid, missing, or not resolved.
     */
    private void validateApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("Stripe API Key is not configured!");
            log.error("Set STRIPE_API_KEY environment variable");
            throw new IllegalStateException(
                    "Stripe API Key is required. Set STRIPE_API_KEY environment variable."
            );
        }

        // Vérifier que ce n'est pas un placeholder non résolu
        if (apiKey.startsWith("${") && apiKey.endsWith("}")) {
            log.error("Stripe API Key is not resolved: {}", apiKey);
            log.error("Make sure STRIPE_API_KEY environment variable is set");
            log.error("In IntelliJ: Run → Edit Configurations → EnvFile → Enable EnvFile");
            throw new IllegalStateException(
                    "Stripe API Key placeholder not resolved. Environment variable not loaded."
            );
        }

        // Vérifier le format
        if (!apiKey.startsWith("sk_test_") && !apiKey.startsWith("sk_live_")) {
            log.error("Invalid Stripe API Key format: {}", maskApiKey(apiKey));
            log.error("💡 Key must start with 'sk_test_' or 'sk_live_'");
            throw new IllegalStateException(
                    "Invalid Stripe API Key format. Must start with 'sk_test_' or 'sk_live_'"
            );
        }

        // Déterminer le mode (test ou live)
        testMode = apiKey.startsWith("sk_test_");

        log.info("Stripe API Key validated");
        log.info("API Key: {}", maskApiKey(apiKey));
        log.info("Mode: {}", testMode ? "TEST" : "LIVE");
    }

    /**
     * Validates the format of the webhook secret used for Stripe integration.
     *
     * This method performs the following validation steps:
     * - Checks that the `webhookSecret` starts with the required prefix `whsec_`.
     *   - Logs a warning if the webhook secret does not follow this format,
     *     indicating that webhook validation may not function as expected.
     * - Logs an informational message if the webhook secret is correctly formatted.
     *
     * This validation ensures that the webhook secret conforms to Stripe's
     * expected format, reducing potential issues during webhook signature verification.
     */
    private void validateWebhookSecret() {
        if (!webhookSecret.startsWith("whsec_")) {
            log.warn("Invalid Webhook Secret format. Should start with 'whsec_'");
            log.warn("Webhook validation may not work correctly");
        } else {
            log.info("Webhook Secret configured");
        }
    }

    /**
     * Logs the current Stripe integration configuration details for debugging and monitoring purposes.
     *
     * This method performs the following actions:
     * - Logs the configured timeout duration for Stripe API requests.
     * - Logs the maximum number of retry attempts for API calls.
     * - Logs the publishable API key. If the key is configured, it is masked for security; otherwise,
     *   a warning message is logged to indicate its absence.
     * - Delegates the logging of relevant environment variables to an auxiliary method.
     *
     * The logged information helps diagnose configuration issues or validate the current setup
     * of the Stripe integration.
     */
    private void logConfiguration() {
        log.info("⏱️  Timeout: {} seconds", timeoutSeconds);
        log.info("🔄 Max Retries: {}", maxRetries);

        if (publishableKey != null && !publishableKey.isEmpty()) {
            log.info("Publishable Key: {}", maskApiKey(publishableKey));
        } else {
            log.info("Publishable Key: NOT CONFIGURED");
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
     * Masks a given API key for security purposes, ensuring only a portion of the key is visible.
     * The masked API key consists of the first 10 characters, followed by "..." and the last 4 characters.
     * If the key is null or shorter than 14 characters, "INVALID_KEY" is returned.
     *
     * @param key the API key to be masked. Must not be null or shorter than 14 characters.
     * @return the masked version of the API key if it's properly formatted, or "INVALID_KEY" otherwise.
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 14) {
            return "INVALID_KEY";
        }
        return key.substring(0, 10) + "..." + key.substring(key.length() - 4);
    }


    /**
     * Checks whether the application is properly configured for Stripe integration.
     *
     * This method evaluates the `apiKey` to determine if it has been set correctly.
     * A valid API Key must:
     * - Not be null
     * - Not be empty
     * - Not be an unresolved placeholder starting with "${"
     * - Start with either "sk_test_" or "sk_live_"
     *
     * @return true if the Stripe API Key is properly configured, false otherwise.
     */
    public boolean isConfigured() {
        return apiKey != null &&
                !apiKey.isEmpty() &&
                !apiKey.startsWith("${") &&
                (apiKey.startsWith("sk_test_") || apiKey.startsWith("sk_live_"));
    }
}