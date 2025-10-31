package com.store.store.service;

import com.store.store.entity.Customer;

/**
 * Service pour gérer les alertes de sécurité.
 * Envoie des notifications aux utilisateurs en cas d'activité suspecte.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-01
 */
public interface ISecurityAlertService {

    /**
     * Notifie un utilisateur d'une possible compromission de compte.
     * Envoie un email d'alerte avec détails de l'incident.
     *
     * @param customer Le customer concerné
     * @param ipAddress L'adresse IP suspecte
     * @param userAgent Le User-Agent utilisé
     * @param incidentType Le type d'incident détecté
     */
    void notifyPossibleAccountCompromise(
            Customer customer,
            String ipAddress,
            String userAgent,
            String incidentType
    );

    /**
     * Notifie un utilisateur d'une connexion depuis un nouvel appareil.
     *
     * @param customer Le customer concerné
     * @param ipAddress L'adresse IP de connexion
     * @param userAgent Le User-Agent du nouvel appareil
     */
    void notifyNewDeviceLogin(
            Customer customer,
            String ipAddress,
            String userAgent
    );

    /**
     * Notifie un utilisateur que tous ses tokens ont été révoqués.
     *
     * @param customer Le customer concerné
     * @param reason La raison de révocation
     */
    void notifyAllTokensRevoked(
            Customer customer,
            String reason
    );
}