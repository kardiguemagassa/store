package com.store.store.service.impl;

import com.store.store.dto.LoginResponseDto;
import com.store.store.dto.UserDto;

import com.store.store.entity.Customer;
import com.store.store.entity.RefreshToken;
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
 * Service implementation for managing refresh tokens.
 * Provides functionality for creating, verifying, refreshing, revoking, and deleting refresh tokens.
 * Also includes utilities for token rotation and security checks.
 *
 * @author Kardigué
 * @version 3.0 - Added refreshAccessToken for HttpOnly cookies
 * @since 2025-11-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceInfoExtractorServiceImpl deviceInfoExtractor;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Value("${store.refresh-token.expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Creates a new refresh token for the specified customer. The refresh token includes
     * various details such as token UUID, expiry date, customer information, IP address,
     * User-Agent, and device information. The token is persisted in the database and returned.
     *
     * @param customer The customer for whom the refresh token is created.
     * @param ipAddress The IP address of the client creating the token.
     * @param userAgent The User-Agent string of the client's browser or application.
     * @return The created and saved {@code RefreshToken} entity.
     */
    @Override
    @Transactional
    public RefreshToken createRefreshToken(Customer customer, String ipAddress, String userAgent) {
        String deviceInfo = deviceInfoExtractor.extractDeviceInfo(userAgent);

        RefreshToken refreshToken = RefreshToken.builder()
                .customer(customer)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceInfo(deviceInfo)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for customer: {} from IP: {}",
                customer.getEmail(), ipAddress);

        return saved;
    }

    /**
     * Verifies the validity of the provided refresh token.
     *
     * This method checks the following conditions:
     * 1. The refresh token exists in the repository.
     * 2. The refresh token is not revoked.
     * 3. The refresh token has not expired.
     *
     * If any of these conditions fail, an {@code IllegalArgumentException} will be thrown.
     *
     * @param token the refresh token to be verified
     * @return the verified {@code RefreshToken} entity
     * @throws IllegalArgumentException if the token is not found, revoked, or expired
     */
    @Override
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            log.warn("Attempted to use revoked refresh token: {}", token);
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Attempted to use expired refresh token: {}", token);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        return refreshToken;
    }

    /**
     * Refreshes the access token for a user based on the provided refresh token, IP address,
     * and User-Agent. This process involves verifying the refresh token, validating the origin
     * of the request, revoking the old refresh token, and generating new tokens.
     *
     * @param token The refresh token provided by the client.
     * @param ipAddress The IP address of the client making the request.
     * @param userAgent The User-Agent string from the client's request.
     * @return A {@code LoginResponseDto} containing the new access token, refresh token,
     *         user information, and token expiry time.
     */
    @Override
    @Transactional
    public LoginResponseDto refreshAccessToken(String token, String ipAddress, String userAgent) {
        log.debug("Attempting to refresh access token from IP: {}", ipAddress);

        // 1. Vérifier la validité du refresh token
        RefreshToken refreshToken = verifyRefreshToken(token);
        Customer customer = refreshToken.getCustomer();

        // 2. SÉCURITÉ: Vérifier l'IP et le User-Agent
        if (!isSameOrigin(refreshToken, ipAddress, userAgent)) {
            log.warn("Token refresh attempt from different IP/UserAgent! " +
                            "Original IP: {}, New IP: {}, Customer: {}",
                    refreshToken.getIpAddress(), ipAddress, customer.getEmail());

            log.info("Allowing refresh despite IP/UserAgent change (user may have changed network)");
        }

        // 3. Révoquer l'ancien refresh token (Token Rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        log.info("Old refresh token revoked for customer: {}", customer.getEmail());

        // 4. Créer un objet Authentication pour générer le JWT
        Authentication authentication = createAuthenticationFromCustomer(customer);

        // 5. Générer un nouveau JWT avec JwtUtil (même méthode que login)
        String newJwtToken = jwtUtil.generateJwtToken(authentication);
        log.debug("New JWT generated for customer: {}", customer.getEmail());

        // 6. Créer un nouveau refresh token
        RefreshToken newRefreshToken = createRefreshToken(customer, ipAddress, userAgent);
        log.info("New refresh token created for customer: {}", customer.getEmail());

        // 7. Construire le UserDto (UTILISE UserMapper)
        UserDto userDto = userMapper.toUserDto(customer, authentication);

        // 8. Construire la réponse (même format que login)
        log.info("Token refresh successful for customer: {}", customer.getEmail());

        return new LoginResponseDto(
                "Token refreshed successfully",
                userDto,
                newJwtToken,
                newRefreshToken.getToken(),
                ACCESS_TOKEN_EXPIRY_SECONDS
        );
    }

    /**
     * Revokes the refresh token identified by the provided token string.
     *
     * This method searches for the refresh token in the repository. If a matching token is found,
     * it marks the token as revoked and updates it in the database. A log entry is also created
     * to indicate that the token has been successfully revoked.
     *
     * @param token the refresh token string that needs to be revoked
     */
    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh token revoked: {}", token);
        });
    }

    /**
     * Revokes all non-revoked refresh tokens associated with a given customer.
     *
     * The method retrieves all active (non-revoked) refresh tokens for the specified customer ID,
     * revokes each of them by setting their 'revoked' property to true,
     * and saves the updated token entities back to the repository. A warning
     * log entry is made to indicate the number of tokens revoked for the customer.
     *
     * @param customerId the ID of the customer whose tokens are to be revoked
     */
    @Override
    @Transactional
    public void revokeAllTokensForCustomer(Long customerId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByCustomer_CustomerIdAndRevokedFalse(customerId);

        tokens.forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });

        log.warn("All {} refresh tokens revoked for customer ID: {}", tokens.size(), customerId);
    }

    /**
     * Deletes all expired refresh tokens from the repository.
     *
     * This method identifies and removes refresh tokens whose expiry date
     * is earlier than the current timestamp. The operation is executed in
     * a transactional context to ensure atomicity. Logs the number of tokens
     * deleted upon execution.
     *
     * @return the number of expired refresh tokens that were successfully deleted
     */
    @Override
    @Transactional
    public int deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Deleted {} expired refresh tokens", deleted);
        return deleted;
    }

    /**
     * Retrieves all active (non-revoked) refresh tokens associated with a specific customer.
     *
     * @param customerId the unique identifier of the customer for whom the active tokens are retrieved
     * @return a list of {@code RefreshToken} entities that are active and associated with the given customer
     */
    @Override
    public List<RefreshToken> getActiveTokensForCustomer(Long customerId) {
        return refreshTokenRepository.findByCustomer_CustomerIdAndRevokedFalse(customerId);
    }

    // MÉTHODES UTILITAIRES PRIVÉES

    /**
     * Creates an {@code Authentication} object from the given {@code Customer} entity.
     *
     * This method wraps the provided {@code Customer} into a {@code CustomerUserDetails} object,
     * which is then used to construct a {@code UsernamePasswordAuthenticationToken} with
     * associated authorities.
     *
     * @param customer the {@code Customer} entity for which the authentication is to be created
     * @return an {@code Authentication} object representing the customer's credentials
     */
    private Authentication createAuthenticationFromCustomer(Customer customer) {
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    /**
     * Determines whether the origin of the current request matches the origin
     * associated with the provided refresh token. The comparison is based on
     * the IP address and User-Agent of the request and the stored refresh token.
     *
     * This method employs a flexible strategy where a match in either the IP address
     * or the User-Agent is deemed sufficient for a successful origin match.
     *
     * @param refreshToken the {@code RefreshToken} object containing the original IP address
     *                     and User-Agent recorded during token creation
     * @param currentIp the current IP address associated with the request
     * @param currentUserAgent the current User-Agent string from the request
     * @return {@code true} if the origin of the request matches the origin associated
     *         with the refresh token (based on IP address or User-Agent); {@code false} otherwise
     */
    private boolean isSameOrigin(RefreshToken refreshToken, String currentIp, String currentUserAgent) {
        // Vérifier l'IP (null-safe)
        boolean sameIp = refreshToken.getIpAddress() == null ||
                currentIp == null ||
                refreshToken.getIpAddress().equals(currentIp);

        // Vérifier le User-Agent (null-safe)
        boolean sameUserAgent = refreshToken.getUserAgent() == null ||
                currentUserAgent == null ||
                refreshToken.getUserAgent().equals(currentUserAgent);

        // Stratégie FLEXIBLE : Au moins un des deux correspond
        return sameIp || sameUserAgent;

        // Stratégie STRICTE (décommenter si on veux bloquer les changements) :
        // return sameIp && sameUserAgent;
    }
}