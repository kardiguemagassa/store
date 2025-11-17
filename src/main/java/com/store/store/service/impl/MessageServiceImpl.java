package com.store.store.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * @author Kardigué
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl {

    private final MessageSource messageSource;


    public String getMessage(String code, Object... args) {
        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            log.warn("Message key '{}' not found for locale '{}'", code, LocaleContextHolder.getLocale());
            return getMessageOrDefault(code, code, args);
        }
    }

    public String getMessageOrDefault(String code, String defaultMessage, Object... args) {
        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            log.debug("Using default message for key '{}': {}", code, defaultMessage);
            return defaultMessage;
        }
    }

    // MÉTHODES DE COMMODITÉ POUR LES RÉPONSES API

    public String getSuccessCreated(String resourceName) {
        return getMessage("api.success.created", resourceName);
    }

    public String getSuccessCreatedWithId(String resourceName, Long id) {
        return getMessage("api.success.created.with.id", resourceName, id);
    }

    public String getSuccessUpdated(String resourceName) {
        return getMessage("api.success.updated", resourceName);
    }

    public String getSuccessUpdatedWithId(String resourceName, Long id) {
        return getMessage("api.success.updated.with.id", resourceName, id);
    }

    public String getSuccessDeleted(String resourceName) {
        return getMessage("api.success.deleted", resourceName);
    }

    public String getSuccessDeletedWithId(String resourceName, Long id) {
        return getMessage("api.success.deleted.with.id", resourceName, id);
    }

    public String getProductCreatedMessage() {
        return getMessage("api.success.product.created");
    }

    public String getOrderCreatedMessage() {
        return getMessage("api.success.order.created");
    }

    public String getProfileUpdatedMessage() {
        return getMessage("api.success.profile.updated");
    }

    public String getNotFoundMessage(String resourceName) {
        return getMessage("api.error.not.found", resourceName);
    }

    public String getNotFoundWithIdMessage(String resourceName, Long id) {
        return getMessage("api.error.not.found.with.id", resourceName, id);
    }

    public String getAlreadyExistsMessage(String resourceName) {
        return getMessage("api.error.already.exists", resourceName);
    }

    public String getProductNotFoundMessage() {
        return getMessage("api.error.product.not.found");
    }

    public String getOrderNotFoundMessage() {
        return getMessage("api.error.order.not.found");
    }

    public String getUserNotFoundMessage() {
        return getMessage("api.error.user.not.found");
    }

    public String getBadCredentialsMessage() {
        return getMessage("api.error.auth.bad.credentials");
    }

    public String getAccountDisabledMessage() {
        return getMessage("api.error.auth.account.disabled");
    }

    public String getAccountLockedMessage() {
        return getMessage("api.error.auth.account.locked");
    }

    public String getValidationErrorMessage() {
        return getMessage("validation.error.message");
    }

    public String getRequiredFieldMessage(String fieldLabel) {
        return getMessage("validation.required", fieldLabel);
    }

    public String getEmailInvalidMessage(String fieldLabel) {
        return getMessage("validation.email", fieldLabel);
    }
}