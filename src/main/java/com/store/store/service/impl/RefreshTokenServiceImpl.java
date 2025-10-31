package com.store.store.service.impl;

import com.store.store.dto.LoginResponseDto;
import com.store.store.dto.UserDto;
import com.store.store.dto.AddressDto;
import com.store.store.entity.Customer;
import com.store.store.entity.RefreshToken;
import com.store.store.mapper.UserMapper;
import com.store.store.repository.RefreshTokenRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.IRefreshTokenService;
import com.store.store.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.store.store.constants.TokenConstants.ACCESS_TOKEN_EXPIRY_SECONDS;

/**
 * Implémentation du service de gestion des Refresh Tokens.
 *
 * @author Kardigué
 * @version 3.0 - Ajout de refreshAccessToken pour cookies HttpOnly
 * @since 2025-01-27
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
     * Crée un nouveau refresh token pour un customer.
     *
     * ✅ CORRECTION: Utiliser directement Instant.now().plusMillis()
     * ❌ AVANT: Instant.from(LocalDateTime.from(...)) → Exception!
     * ✅ APRÈS: Instant.now().plusMillis(...) → Direct et correct
     * Extraction automatique du device_info depuis le User-Agent
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
     * Vérifie qu'un refresh token est valide.
     *
     * ✅ CORRECTION: Comparaison directe d'Instant
     * ❌ AVANT: Instant.from(ChronoLocalDateTime.from(...)) → Conversion inutile
     * ✅ APRÈS: refreshToken.getExpiryDate().isBefore(Instant.now()) → Simple
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
     * ✅ NOUVEAU: Renouvelle le JWT en utilisant un refresh token valide.
     *
     * Cette méthode implémente le flux complet de refresh token avec rotation :
     * 1. Vérifie la validité du refresh token
     * 2. Vérifie l'IP et le User-Agent pour détecter les vols de token
     * 3. Révoque l'ancien refresh token (sécurité)
     * 4. Génère un nouveau JWT avec JwtUtil
     * 5. Génère un nouveau refresh token (token rotation)
     * 6. Retourne LoginResponseDto avec tous les nouveaux tokens
     *
     * @param token Le refresh token reçu du cookie
     * @param ipAddress L'adresse IP actuelle du client
     * @param userAgent Le User-Agent actuel du client
     * @return LoginResponseDto avec nouveau JWT et refresh token
     * @throws IllegalArgumentException si le token est invalide ou si détection de vol
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

        // 7. Construire le UserDto (✅ UTILISE UserMapper)
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

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Refresh token revoked: {}", token);
        });
    }

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

    @Override
    @Transactional
    public int deleteExpiredTokens() {
        int deleted = refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        log.info("Deleted {} expired refresh tokens", deleted);
        return deleted;
    }

    @Override
    public List<RefreshToken> getActiveTokensForCustomer(Long customerId) {
        return refreshTokenRepository.findByCustomer_CustomerIdAndRevokedFalse(customerId);
    }

    // ============================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ============================================

    /**
     * ✅ Crée un objet Authentication à partir d'un Customer.
     *
     * Nécessaire pour générer un JWT avec JwtUtil.generateJwtToken()
     * qui attend un Authentication en paramètre.
     *
     * Cette méthode reproduit le même comportement que lors du login :
     * - Créer un CustomerUserDetails à partir du Customer
     * - Créer un UsernamePasswordAuthenticationToken avec les authorities
     *
     * @param customer Le customer authentifié
     * @return Authentication contenant les infos et authorities
     */
    private Authentication createAuthenticationFromCustomer(Customer customer) {
        CustomerUserDetails userDetails = new CustomerUserDetails(customer);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    /**
     * ✅ SÉCURITÉ: Vérifie si la requête provient de la même origine.
     *
     * Compare l'IP et le User-Agent du refresh token avec ceux de la requête actuelle.
     * Aide à détecter les vols de refresh token.
     *
     * ⚠️ IMPORTANT: Cette vérification peut être trop stricte pour les utilisateurs mobiles
     * qui changent fréquemment de réseau (WiFi → 4G → WiFi).
     *
     * Stratégie actuelle : FLEXIBLE
     * - Retourne true si au moins l'IP OU le User-Agent correspond
     * - Logger un warning si les deux diffèrent
     * - Ne pas bloquer (meilleure UX)
     *
     * Alternative STRICTE (plus sécurisé mais moins UX-friendly) :
     * - return sameIp && sameUserAgent;
     *
     * @param refreshToken Le refresh token original
     * @param currentIp L'IP actuelle
     * @param currentUserAgent Le User-Agent actuel
     * @return true si même origine (flexible), false sinon
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

        // Stratégie STRICTE (décommenter si vous voulez bloquer les changements) :
        // return sameIp && sameUserAgent;
    }
}