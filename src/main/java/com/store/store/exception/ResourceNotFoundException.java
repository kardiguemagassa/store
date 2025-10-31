package com.store.store.exception;

/**
 * Exception levée lorsqu'une ressource n'est pas trouvée en base de données.
 *
 * Exemples d'utilisation :
 * - Customer introuvable par ID
 * - Product introuvable par SKU
 * - Order introuvable par numéro
 *
 * @author Kardigué
 * @version 1.0
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructeur avec 3 paramètres (ressource, champ, valeur).
     *
     * Utilisé par ExceptionFactory.resourceNotFound(String, String, Object)
     *
     * @param resourceName Nom de la ressource (ex: "Customer", "Product")
     * @param fieldName Nom du champ (ex: "id", "email")
     * @param fieldValue Valeur recherchée
     *
     * @example
     * throw new ResourceNotFoundException("Customer", "email", "test@example.com");
     * // Message: "Customer introuvable avec les données email : 'test@example.com'"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s introuvable avec les données %s : '%s'", resourceName, fieldName, fieldValue));
    }

    /**
     * Constructeur avec message personnalisé.
     *
     * Utilisé par ExceptionFactory.resourceNotFound(String)
     *
     * @param message Message d'erreur personnalisé
     *
     * @example
     * throw new ResourceNotFoundException("Client introuvable");
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructeur avec message et cause.
     *
     * Utilisé pour wrapper une autre exception.
     *
     * @param message Message d'erreur
     * @param cause Exception d'origine
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}