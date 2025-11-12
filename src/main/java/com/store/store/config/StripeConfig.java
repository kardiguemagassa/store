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
     * Configure le SDK Stripe avec les paramètres d'initialisation appropriés.
     * Cette méthode configure le SDK Stripe à l'aide des paramètres
     * fournis par l'objet `stripeProperties`. Les actions suivantes sont effectuées :
     * 1. Définit la clé API Stripe à partir de `stripeProperties`.
     * 2. Configure les délais d'expiration de connexion et de lecture en fonction de la valeur de `timeoutSeconds`
     * dans `stripeProperties`. Le délai d'expiration est appliqué uniquement si la valeur n'est pas nulle.
     * 3. Configure le nombre maximal de tentatives de connexion réseau en fonction de la valeur de `maxRetries`
     * dans `stripeProperties`. Ceci est appliqué uniquement si la valeur n'est pas nulle.
     * 4. Consigne le mode de fonctionnement actuel de Stripe (TEST ou LIVE) en fonction de la méthode
     * `isTestMode` dans `stripeProperties`. En mode LIVE, un avertissement
     * est consigné pour indiquer que les transactions en direct sont activées.
     * Cela garantit que Stripe est correctement configuré avant toute transaction
     * requête. La méthode consigne également les détails de configuration pertinents pour le débogage
     * et la surveillance opérationnelle.
     * Remarque : Cette méthode est conçue pour être appelée lors de l'initialisation de l'application
     *, généralement dans le cadre d'un processus d'installation ou de configuration.
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
     * Vérifie si la configuration Stripe est correcte.
     * Cette méthode vérifie si les propriétés requises pour la configuration Stripe,
     * telles que la clé API, ont été correctement initialisées et sont valides.
     * @return true si la configuration Stripe est valide et prête à l'emploi, false sinon.
     */
    public boolean isConfigured() {
        return stripeProperties.isConfigured();
    }

    /**
     * Détermine si l'application est exécutée en mode test pour la configuration Stripe.
     * Cette méthode récupère le statut du mode test à partir des propriétés Stripe
     * permettant à l'application de différencier les environnements de test et de production.
     * @return true si l'application est actuellement en mode test, false sinon.
     */
    public boolean isTestMode() {
        return stripeProperties.isTestMode();
    }

    /**
     * Récupère l'objet StripeProperties associé à la configuration actuelle.
     * @return l'objet StripeProperties contenant les détails de configuration de Stripe.
     */
    public StripeProperties getProperties() {
        return stripeProperties;
    }
}