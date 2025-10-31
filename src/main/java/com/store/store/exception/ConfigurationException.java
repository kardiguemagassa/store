package com.store.store.exception;

/**
 * Exception levée en cas d'erreur de configuration système.
 *
 * Exemples d'utilisation :
 * - Fichier de configuration manquant
 * - Paramètre de configuration invalide
 * - Service externe mal configuré
 *
 * @author Kardigué
 * @version 1.0
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Constructeur avec message simple.
     *
     * @param message Message d'erreur décrivant le problème de configuration
     *
     * @example
     * throw new ConfigurationException("Le fichier application.yml est manquant");
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause.
     *
     * Utilisé par ExceptionFactory.configurationError(String, Throwable)
     *
     * @param message Message d'erreur
     * @param cause Exception d'origine
     *
     * @example
     * throw new ConfigurationException("Erreur de configuration JWT", ioException);
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}