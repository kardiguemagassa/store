package com.store.store.exception;

import com.store.store.constants.ErrorCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory pour créer des exceptions de manière cohérente.
 *
 * Centralise la création d'exceptions pour garantir :
 * - Messages i18n cohérents
 * - Logging approprié
 * - Structure d'erreur standardisée
 *
 * @author Kardigué
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ExceptionFactory {

    private final MessageSource messageSource;

    // ========================================================================
    // VALIDATION EXCEPTIONS
    // ========================================================================

    /**
     * Crée une ValidationException pour une seule erreur de champ.
     *
     * Utilisé quand un seul champ est invalide.
     *
     * @param field Nom du champ en erreur
     * @param message Message d'erreur (déjà localisé)
     * @return ValidationException prête à être lancée
     *
     * @example
     * throw exceptionFactory.validationError("email", "L'email est invalide");
     */
    public ValidationException validationError(String field, String message) {
        return new ValidationException(field, message);
    }

    /**
     * Crée une ValidationException pour plusieurs erreurs de champs.
     *
     * Utilisé quand plusieurs champs sont invalides.
     *
     * @param errors Map des erreurs (field → message)
     * @return ValidationException prête à être lancée
     *
     * @example
     * Map<String, String> errors = new HashMap<>();
     * errors.put("email", "L'email existe déjà");
     * errors.put("mobile", "Le mobile existe déjà");
     * throw exceptionFactory.validationError(errors);
     */
    public ValidationException validationError(Map<String, String> errors) {
        return new ValidationException(errors);
    }

    /**
     * Crée une ValidationException avec un code de message à localiser.
     *
     * @param field Nom du champ en erreur
     * @param messageCode Code du message dans messages.properties
     * @param args Arguments optionnels pour le message
     * @return ValidationException prête à être lancée
     *
     * @example
     * throw exceptionFactory.validationErrorWithCode(
     *     "email",
     *     "validation.email.invalid"
     * );
     */
    public ValidationException validationErrorWithCode(String field, String messageCode, Object... args) {
        String localizedMessage = getLocalizedMessage(messageCode, args);
        return new ValidationException(field, localizedMessage);
    }

    // ========================================================================
    // BUSINESS EXCEPTIONS
    // ========================================================================

    /**
     * Crée une BusinessException avec un message simple.
     *
     * Utilisé pour les erreurs métier générales.
     *
     * @param message Message d'erreur (déjà localisé)
     * @return BusinessException prête à être lancée
     *
     * @example
     * throw exceptionFactory.businessError("Stock insuffisant");
     */
    public BusinessException businessError(String message) {
        return new BusinessException(message);
    }

    /**
     * Crée une BusinessException avec un code de message à localiser.
     *
     * @param messageCode Code du message dans messages.properties
     * @param args Arguments optionnels pour le message
     * @return BusinessException prête à être lancée
     *
     * @example
     * throw exceptionFactory.businessErrorWithCode(
     *     "error.stock.insufficient",
     *     productName
     * );
     */
    public BusinessException businessErrorWithCode(String messageCode, Object... args) {
        String localizedMessage = getLocalizedMessage(messageCode, args);
        return new BusinessException(localizedMessage);
    }

    // ========================================================================
    // RESOURCE NOT FOUND EXCEPTIONS
    // ========================================================================

    /**
     * Crée une ResourceNotFoundException.
     *
     * Utilisé quand une ressource n'est pas trouvée en base.
     *
     * @param resourceName Nom de la ressource (ex: "Customer", "Product")
     * @param fieldName Nom du champ de recherche (ex: "id", "email")
     * @param fieldValue Valeur recherchée
     * @return ResourceNotFoundException prête à être lancée
     *
     * @example
     * throw exceptionFactory.resourceNotFound("Customer", "email", "test@example.com");
     * // Message: "Customer non trouvé avec email: test@example.com"
     */
    public ResourceNotFoundException resourceNotFound(String resourceName, String fieldName, Object fieldValue) {
        String message = String.format(
                "%s non trouvé avec %s: %s",
                resourceName,
                fieldName,
                fieldValue
        );
        return new ResourceNotFoundException(message);
    }

    /**
     * Crée une ResourceNotFoundException avec un message personnalisé.
     *
     * @param message Message d'erreur personnalisé
     * @return ResourceNotFoundException prête à être lancée
     */
    public ResourceNotFoundException resourceNotFound(String message) {
        return new ResourceNotFoundException(message);
    }

    /**
     * Crée une ResourceNotFoundException avec un code de message à localiser.
     *
     * @param messageCode Code du message dans messages.properties
     * @param args Arguments optionnels pour le message
     * @return ResourceNotFoundException prête à être lancée
     *
     * @example
     * throw exceptionFactory.resourceNotFoundWithCode(
     *     "error.customer.not.found",
     *     customerId
     * );
     */
    public ResourceNotFoundException resourceNotFoundWithCode(String messageCode, Object... args) {
        String localizedMessage = getLocalizedMessage(messageCode, args);
        return new ResourceNotFoundException(localizedMessage);
    }

    // ========================================================================
    // CONFIGURATION EXCEPTIONS
    // ========================================================================

    /**
     * Crée une ConfigurationException.
     *
     * Utilisé pour les erreurs de configuration système.
     *
     * @param message Message d'erreur
     * @return ConfigurationException prête à être lancée
     */
    public ConfigurationException configurationError(String message) {
        return new ConfigurationException(message);
    }

    /**
     * Crée une ConfigurationException avec cause.
     *
     * @param message Message d'erreur
     * @param cause Exception d'origine
     * @return ConfigurationException prête à être lancée
     */
    public ConfigurationException configurationError(String message, Throwable cause) {
        return new ConfigurationException(message, cause);
    }

    // ========================================================================
    // ROLE EXCEPTIONS
    // ========================================================================

    /**
     * Crée une ResourceNotFoundException pour un rôle manquant.
     *
     * Utilisé quand un rôle requis n'existe pas en base.
     *
     * @param roleName Nom du rôle manquant
     * @return ResourceNotFoundException prête à être lancée
     *
     * @example
     * throw exceptionFactory.missingRole("ROLE_USER");
     * // Message: "Le rôle ROLE_USER n'existe pas en base de données"
     */
    public ResourceNotFoundException missingRole(String roleName) {
        String message = String.format(
                "Le rôle %s n'existe pas en base de données. " +
                        "Veuillez vérifier les données initiales.",
                roleName
        );
        return new ResourceNotFoundException(message);
    }

    // ========================================================================
    // FILE STORAGE EXCEPTIONS
    // ========================================================================

    /**
     * Utilisé pour les erreurs de stockage de fichiers.
     *
     * @param message Message d'erreur
     * @return FileStorageException prête à être lancée
     */
    public FileStorageException fileStorageError(String message) {
        return new FileStorageException(message);
    }

    /**
     * avec cause.
     *
     * @param message Message d'erreur
     * @param cause Exception d'origine
     * @return FileStorageException prête à être lancée
     */
    public FileStorageException fileStorageError(String message, Throwable cause) {
        return new FileStorageException(message, cause);
    }

    /**
     * avec un code de message à localiser.
     *
     * @param messageCode Code du message dans messages.properties
     * @param args Arguments optionnels pour le message
     * @return FileStorageException prête à être lancée
     */
    public FileStorageException fileStorageErrorWithCode(String messageCode, Object... args) {
        String localizedMessage = getLocalizedMessage(messageCode, args);
        return new FileStorageException(localizedMessage);
    }

    // ========================================================================
    // MÉTHODES UTILITAIRES
    // ========================================================================

    /**
     * Récupère un message localisé depuis messages.properties.
     *
     * @param code Code du message
     * @param args Arguments optionnels
     * @return Message traduit dans la locale courante
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}