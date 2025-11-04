package com.store.store.service;

import com.store.store.dto.LoginResponseDto;
import com.store.store.entity.Customer;
import com.store.store.entity.RefreshToken;

import java.util.List;

/**
 * Interface définissant les opérations pour gérer les refresh tokens.
 * Les refresh tokens sont utilisés pour renouveler des JWT (JSON Web Tokens)
 * de manière sécurisée et gérer les sessions utilisateur.
 *
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