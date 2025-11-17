package com.store.store.service.impl;

import com.store.store.dto.auth.LoginResponseDto;
import com.store.store.dto.user.UserDto;
import com.store.store.entity.Customer;
import com.store.store.entity.RefreshToken;
import com.store.store.exception.ExceptionFactory;
import com.store.store.mapper.UserMapper;
import com.store.store.repository.RefreshTokenRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.IRefreshTokenService;

import com.store.store.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.store.store.constants.TokenConstants.ACCESS_TOKEN_EXPIRY_SECONDS;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceInfoExtractorServiceImpl deviceInfoExtractor;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    @Value("${store.refresh-token.expiration-ms:604800000}") // 7 jours par défaut
    private long refreshTokenExpirationMs;

    // CRÉATION DE REFRESH TOKEN

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Customer customer, String ipAddress, String userAgent) {
        // Extraction des informations d'appareil
        String deviceInfo = deviceInfoExtractor.extractDeviceInfo(userAgent);

        // Construction du refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .customer(customer)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceInfo(deviceInfo)
                .build();

        // Sauvegarde en base
        RefreshToken saved = refreshTokenRepository.save(refreshToken);

        log.info("Refresh token created for customer: {} from IP: {} (Device: {})", customer.getEmail(), ipAddress, deviceInfo);

        return saved;
    }

    // VÉRIFICATION DE REFRESH TOKEN

    @Override
    public RefreshToken verifyRefreshToken(String token) {
        // Vérification existence
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found: {}", token);
                    //  Utilisation de messageService
                    return new IllegalArgumentException(messageService.getMessage("error.auth.refresh.token.not.found"));
                });

        // Vérification révocation
        if (refreshToken.isRevoked()) {
            log.warn("Attempted to use revoked refresh token: {} (Customer: {})",
                    token, refreshToken.getCustomer().getEmail());
            // Utilisation de messageService
            throw new IllegalArgumentException(messageService.getMessage("error.auth.refresh.token.revoked"));
        }

        // Vérification expiration
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Attempted to use expired refresh token: {} (Expired: {})", token, refreshToken.getExpiryDate());
            //Utilisation de messageService
            throw new IllegalArgumentException(messageService.getMessage("error.auth.refresh.token.expired"));
        }

        log.debug("Refresh token verified successfully for customer: {}", refreshToken.getCustomer().getEmail());

        return refreshToken;
    }

    // RAFRAÎCHISSEMENT D'ACCESS TOKEN

    @Override
    @Transactional
    public LoginResponseDto refreshAccessToken(String token, String ipAddress, String userAgent) {
        log.debug("Attempting to refresh access token from IP: {}", ipAddress);

        // 1. Vérifier la validité du refresh token
        RefreshToken refreshToken = verifyRefreshToken(token);
        Customer customer = refreshToken.getCustomer();

        // 2. SÉCURITÉ : Vérifier l'IP et le User-Agent
        if (!isSameOrigin(refreshToken, ipAddress, userAgent)) {
            log.warn("Token refresh from DIFFERENT origin! " +
                            "Original IP: {} → New IP: {} | " +
                            "Original UA: {} → New UA: {} | " +
                            "Customer: {}",
                    refreshToken.getIpAddress(), ipAddress,
                    refreshToken.getUserAgent(), userAgent,
                    customer.getEmail());

            log.info("Allowing refresh despite origin change (FLEXIBLE mode)");
            // Note : En mode STRICT, on lancerait une exception ici
        }

        // 3. Révoquer l'ancien refresh token (Token Rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        log.info("Old refresh token revoked for customer: {} (Token Rotation)", customer.getEmail());

        // 4. Créer un objet Authentication pour générer le JWT
        Authentication authentication = createAuthenticationFromCustomer(customer);

        // 5. Générer un nouveau JWT avec JwtUtil (même méthode que login)
        String newJwtToken = jwtUtil.generateJwtToken(authentication);
        log.debug("New JWT generated for customer: {}", customer.getEmail());

        // 6. Créer un nouveau refresh token
        RefreshToken newRefreshToken = createRefreshToken(customer, ipAddress, userAgent);
        log.info("New refresh token created for customer: {}", customer.getEmail());

        // 7. Construire le UserDto (utilise UserMapper)
        UserDto userDto = userMapper.toUserDto(customer, authentication);

        // 8. Construire la réponse (même format que login)
        log.info("Token refresh successful for customer: {} from IP: {}", customer.getEmail(), ipAddress);

        return new LoginResponseDto(
                "Token refreshed successfully",
                userDto,
                newJwtToken,
                newRefreshToken.getToken(),
                ACCESS_TOKEN_EXPIRY_SECONDS
        );
    }

    // RÉVOCATION DE TOKENS

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh token revoked: {} (Customer: {})", token, refreshToken.getCustomer().getEmail());
        });
    }


    @Override
    @Transactional
    public void revokeAllTokensForCustomer(Long customerId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByCustomer_CustomerIdAndRevokedFalse(customerId);

        tokens.forEach(token -> {token.setRevoked(true);refreshTokenRepository.save(token);});

        log.warn("ALL {} refresh tokens revoked for customer ID: {} (Full logout)", tokens.size(), customerId);
    }

    // NETTOYAGE DES TOKENS

    @Override
    @Transactional
    public int deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info(" Cleanup: {} expired refresh tokens deleted", deleted);
        return deleted;
    }

    // CONSULTATION DES TOKENS

    @Override
    public List<RefreshToken> getActiveTokensForCustomer(Long customerId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByCustomer_CustomerIdAndRevokedFalse(customerId);

        log.debug("Found {} active refresh tokens for customer ID: {}", tokens.size(), customerId);
        return tokens;
    }

    // MÉTHODES PRIVÉES - UTILITAIRES

    private Authentication createAuthenticationFromCustomer(Customer customer) {
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private boolean isSameOrigin(RefreshToken refreshToken, String currentIp, String currentUserAgent) {
        // Vérifier l'IP (null-safe)
        boolean sameIp = refreshToken.getIpAddress() == null || currentIp == null || refreshToken.getIpAddress().equals(currentIp);

        // Vérifier le User-Agent (null-safe)
        boolean sameUserAgent = refreshToken.getUserAgent() == null || currentUserAgent == null ||
                refreshToken.getUserAgent().equals(currentUserAgent);

        // Stratégie FLEXIBLE : Au moins un des deux correspond
        return sameIp || sameUserAgent;
    }

}