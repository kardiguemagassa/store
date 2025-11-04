package com.store.store.service;

import com.store.store.entity.Customer;

/**
 * Interface representing a service for sending security alert notifications
 * to customers regarding suspicious activities and account-related incidents.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
public interface ISecurityAlertService {

    void notifyPossibleAccountCompromise(Customer customer, String ipAddress, String userAgent, String incidentType);
    void notifyNewDeviceLogin(Customer customer, String ipAddress, String userAgent);
    void notifyAllTokensRevoked(Customer customer, String reason);
}