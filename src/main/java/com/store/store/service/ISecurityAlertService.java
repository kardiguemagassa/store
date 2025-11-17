package com.store.store.service;

import com.store.store.entity.Customer;

/**
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
public interface ISecurityAlertService {

    void notifyPossibleAccountCompromise(Customer customer, String ipAddress, String userAgent, String incidentType);
    void notifyNewDeviceLogin(Customer customer, String ipAddress, String userAgent);
    void notifyAllTokensRevoked(Customer customer, String reason);
}