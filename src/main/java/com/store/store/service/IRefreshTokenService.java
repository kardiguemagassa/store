package com.store.store.service;

import com.store.store.dto.auth.LoginResponseDto;
import com.store.store.entity.Customer;
import com.store.store.entity.RefreshToken;

import java.util.List;

/**
 * @author Kardigué
 * @version 3.0 - Added refreshAccessToken for HttpOnly cookies
 * @since 2025-11-01
 */
public interface IRefreshTokenService {

    RefreshToken createRefreshToken(Customer customer, String ipAddress, String userAgent);
    RefreshToken verifyRefreshToken(String token);
    LoginResponseDto refreshAccessToken(String token, String ipAddress, String userAgent);
    void revokeRefreshToken(String token);
    void revokeAllTokensForCustomer(Long customerId);
    int deleteExpiredTokens();
    List<RefreshToken> getActiveTokensForCustomer(Long customerId);
}