package com.store.store.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import jakarta.validation.constraints.NotBlank;

/**
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
     * Représente la clé API secrète pour l'intégration Stripe.
     * Cette clé est requise pour authentifier les requêtes à l'API Stripe. Elle doit être
     * fournie et définie via la variable d'environnement `STRIPE_API_KEY`. La valeur
     * ne peut pas être vide ni nulle.
     * Une validation est effectuée pour garantir que la clé est correctement configurée avant toute utilisation
     * pour les opérations Stripe.

     */
    @NotBlank(message = "Stripe API Key is required. Set STRIPE_API_KEY environment variable.")
    private String apiKey;

    /**
     * Représente la clé secrète utilisée pour valider les requêtes webhook entrantes de Stripe.
     * Cette clé secrète est fournie par Stripe lors de la configuration des webhooks et sert à garantir
     * que les données des webhooks sont authentiques et proviennent bien de Stripe.
     * Il est recommandé de conserver cette valeur en lieu sûr et de ne pas l'exposer dans les journaux
     * ni dans des emplacements non sécurisés. La validation de cette valeur garantit l'intégrité et
     * la sécurité des événements webhook gérés par l'application.
     */
    private String webhookSecret;

    /**
     * Clé publique permettant d'authentifier les requêtes API Stripe côté client.
     * Cette clé peut être utilisée en toute sécurité dans des environnements publics, tels que les clients web ou mobiles,
     * et sert à des opérations comme la création de jetons ou les interactions de base côté client
     * avec l'API Stripe.
     */
    private String publishableKey;

    /**
     * Représente la durée du délai d'attente, en secondes, pour les opérations liées à la configuration de Stripe
     * ou à son intégration. Ce paramètre détermine la durée pendant laquelle l'application attend une réponse
     * avant d'expirer. Il est couramment utilisé pour les appels réseau ou API.
     * Valeur par défaut : 30 secondes.
     */
    private Integer timeoutSeconds = 30;

    /**
     * Nombre maximal de tentatives de nouvelle connexion en cas d'échec.
     * Cette variable définit la limite supérieure des tentatives de nouvelle connexion pour les opérations
     * rencontrant des erreurs non critiques, telles que des délais d'attente réseau ou des limitations de débit d'API.
     * Elle contribue à garantir une gestion robuste des problèmes transitoires tout en évitant
     * des tentatives infinies, susceptibles d'entraîner une surcharge inutile.
     * Valeur par défaut : 3

     */
    private Integer maxRetries = 3;

    /**
     * Indique si l'application est exécutée en mode test.
     * Si la valeur est «true», l'intégration Stripe fonctionne dans un environnement de test (sandbox),
     * permettant d'effectuer des tests sans transactions financières réelles.
     */
    private boolean testMode = true;

    /**
     * Valide et enregistre la configuration de l'intégration Stripe.
     * Cette méthode effectue les tâches suivantes :
     * Valide la clé API Stripe pour s'assurer qu'elle est correctement configurée et respecte le format attendu.
     * Valide le secret du webhook, s'il est fourni, en s'assurant qu'il respecte les conventions de format attendues.
     * Enregistre la configuration Stripe, y compris les paramètres de délai d'expiration, les tentatives de nouvelle connexion et autres détails pertinents.
     * Enregistre des messages informatifs pour indiquer la réussite ou les avertissements concernant la configuration.
     * Lève :
     * Une exception IllegalStateException si la clé API est invalide ou non configurée.
     * Remarque : Cette méthode est annotée avec `@PostConstruct` pour être exécutée
     * automatiquement après la phase d'initialisation du bean dans un contexte Spring.
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
     * Valide la configuration de la clé API Stripe.
     * Cette méthode vérifie que le champ `apiKey` est correctement configuré, non vide,
     * résolu et respecte les conventions de format attendues pour les clés API Stripe.
     * Le processus de validation comprend :
     * 1. Vérification si la clé API est nulle ou vide.
     * - Consigne l'erreur et lève une exception IllegalStateException si elle n'est pas configurée.
     * 2. Vérification que la clé API n'est pas un espace réservé non résolu.
     * - Consigne l'erreur et lève une exception IllegalStateException si elle n'est pas initialisée.
     * 3. Vérification que le format de la clé API commence par `sk_test_` ou `sk_live_`.
     * Consigne l'erreur et lève une exception IllegalStateException si le format est invalide.
     * De plus, cette méthode détermine le mode de fonctionnement de la clé API
     * (mode Test ou Production) et consigne la clé API masquée à des fins de débogage.
     * Consigne les messages informatifs relatifs à la validation réussie ou aux problèmes de configuration.
     * - IllegalStateException si la clé `apiKey` est invalide, manquante ou non résolue.
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
     * Valide le format du secret webhook utilisé pour l'intégration Stripe.
     * Cette méthode effectue les étapes de validation suivantes :
     * Vérifie que `webhookSecret` commence par le préfixe requis `whsec_`.
     * Consigne un avertissement si le secret webhook ne respecte pas ce format,
     * indiquant que la validation du webhook risque de ne pas fonctionner correctement.
     * Consigne un message d'information si le secret webhook est correctement formaté.
     * Cette validation garantit que le secret webhook est conforme au format attendu par Stripe,
     * réduisant ainsi les problèmes potentiels lors de la vérification de la signature du webhook.
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
     * Enregistre les détails de la configuration actuelle de l'intégration Stripe à des fins de débogage et de surveillance.
     * Cette méthode effectue les actions suivantes :
     * Enregistre la durée du délai d'attente configuré pour les requêtes API Stripe.
     * Enregistre le nombre maximal de tentatives de nouvelle connexion pour les appels API.
     * Enregistre la clé API publique. Si la clé est configurée, elle est masquée pour des raisons de sécurité ; sinon,
     * un message d'avertissement est enregistré pour indiquer son absence.
     * Délègue l'enregistrement des variables d'environnement pertinentes à une méthode auxiliaire.
     * Les informations enregistrées permettent de diagnostiquer les problèmes de configuration ou de valider la configuration actuelle
     * de l'intégration Stripe.
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
     * Masque une clé API donnée pour des raisons de sécurité, en ne laissant apparaître qu'une partie de la clé.
     * La clé API masquée est composée des 10 premiers caractères, suivis de "..." et des 4 derniers caractères.
     * Si la clé est nulle ou inférieure à 14 caractères, la fonction renvoie "INVALID_KEY".
     * @param key la clé API à masquer. Ne doit pas être nulle ni inférieure à 14 caractères.
     * @return la version masquée de la clé API si elle est correctement formatée, ou "INVALID_KEY" dans le cas contraire.
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 14) {
            return "INVALID_KEY";
        }
        return key.substring(0, 10) + "..." + key.substring(key.length() - 4);
    }

    /**
     * Vérifie si l'application est correctement configurée pour l'intégration Stripe.
     * Cette méthode évalue la clé API (`apiKey`) pour déterminer si elle a été correctement définie.
     * Une clé API valide doit :
     * Ne pas être nulle
     * Ne pas être vide
     * Ne pas être un espace réservé non résolu commençant par "${"
     * Commencer par "sk_test_" ou "sk_live_"
     * @return true si la clé API Stripe est correctement configurée, false sinon.
     */
    public boolean isConfigured() {
        return apiKey != null &&
                !apiKey.isEmpty() &&
                !apiKey.startsWith("${") &&
                (apiKey.startsWith("sk_test_") || apiKey.startsWith("sk_live_"));
    }
}