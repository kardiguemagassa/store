package com.store.store.service.impl;

import com.store.store.dto.LoginRequestDto;
import com.store.store.dto.LoginResponseDto;
import com.store.store.dto.RegisterRequestDto;
import com.store.store.dto.UserDto;
import com.store.store.dto.AddressDto;
import com.store.store.entity.Customer;
import com.store.store.entity.RefreshToken;
import com.store.store.entity.Role;
import com.store.store.exception.ExceptionFactory;
import com.store.store.mapper.UserMapper;
import com.store.store.repository.CustomerRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.IAuthService;
import com.store.store.service.IRefreshTokenService;
import com.store.store.service.IRoleAssignmentService;
import com.store.store.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.store.store.constants.TokenConstants.ACCESS_TOKEN_EXPIRY_SECONDS;

/**
 * Service d'authentification gérant l'inscription et la connexion des utilisateurs.
 *
 * Responsabilités :
 * - Inscription de nouveaux utilisateurs avec validation complète
 * - Connexion des utilisateurs avec génération de JWT + Refresh Token
 * - Vérification des mots de passe compromis
 * - Gestion des doublons (email, mobile)
 *
 * @author Kardigué
 * @version 3.0 (JWT + Refresh Token + Cookies)
 * @since 2025-01-27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    private final IRoleAssignmentService roleAssignmentService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final IRefreshTokenService refreshTokenService;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;
    private final UserMapper userMapper;

    /**
     * Inscrit un nouvel utilisateur après validation complète.
     *
     * Étapes :
     * 1. Vérifier que le mot de passe n'est pas compromis
     * 2. Vérifier qu'il n'y a pas de doublons (email/mobile)
     * 3. Créer le customer avec le mot de passe hashé
     * 4. Assigner le rôle USER par défaut
     * 5. Sauvegarder en base
     *
     * @param request Données d'inscription validées par Jakarta
     * @throws com.store.store.exception.ValidationException si le mot de passe est compromis ou si des doublons existent
     */
    @Override
    @Transactional
    public void registerUser(RegisterRequestDto request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        // 1. Vérifier mot de passe compromis
        validatePasswordNotCompromised(request.getPassword(), request.getEmail());

        // 2. Vérifier doublons
        checkNoDuplicates(request);

        // 3. Créer le customer
        Customer customer = createCustomer(request);

        // 4. Assigner rôles
        assignInitialRoles(customer, request);

        // 5. Sauvegarder
        customerRepository.save(customer);

        log.info("User registered successfully: {}", customer.getEmail());
    }

    /**
     * Authentifie un utilisateur et génère un token JWT + Refresh Token.
     *
     * VERSION 3.0: Cette méthode accepte maintenant 3 paramètres séparés:
     * 1. LoginRequestDto - Identifiants de connexion
     * 2. String ipAddress - Adresse IP du client (pour tracking et sécurité)
     * 3. String userAgent - User-Agent du navigateur (pour détection d'anomalies)
     *
     * Elle crée DEUX tokens:
     * 1. Access Token (JWT) - 15 minutes
     * 2. Refresh Token (UUID) - 7 jours, stocké en base de données avec IP et User-Agent
     *
     * @param request Identifiants de connexion (username, password)
     * @param ipAddress Adresse IP du client (pour audit et sécurité)
     * @param userAgent User-Agent du navigateur (pour détecter les anomalies)
     * @return LoginResponseDto contenant les infos utilisateur, JWT et refresh token
     * @throws org.springframework.security.core.AuthenticationException si les identifiants sont invalides
     */
    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent) {
        log.debug("Login attempt for username: {} from IP: {}", request.username(), ipAddress);

        // 1. Authentification Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // 2. Récupérer le customer authentifié
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Customer loggedInUser = userDetails.customer();

        // 3. Construire le UserDto  avec UserMapper
        UserDto userDto = userMapper.toUserDto(loggedInUser, authentication);

        // 4. Générer Access Token (JWT 15 min)
        String jwtToken = jwtUtil.generateJwtToken(authentication);
        log.info("Access token generated for user: {}", loggedInUser.getEmail());

        // 5. Créer Refresh Token (UUID 7 jours)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                loggedInUser,
                ipAddress,
                userAgent
        );

        log.info("✅ Refresh token created and stored in database for user: {} from IP: {}",
                loggedInUser.getEmail(), ipAddress);

        // 6. Construire la réponse
        log.info("Login successful for user: {} from IP: {}", loggedInUser.getEmail(), ipAddress);

        return new LoginResponseDto(
                "Login successful",     // 1. message
                userDto,                        // 2. user
                jwtToken,                       // 3. jwtToken
                refreshToken.getToken(),        // 4. refreshToken (uuid)
                ACCESS_TOKEN_EXPIRY_SECONDS     //. expireIn (900) 15 min
        );
    }

    // MÉTHODES PRIVÉES - VALIDATION
    /**
     * Vérifie que le mot de passe n'est pas compromis via HaveIBeenPwned API.
     *
     * @param password Mot de passe à vérifier
     * @param email Email de l'utilisateur (pour le logging)
     * @throws com.store.store.exception.ValidationException si le mot de passe est compromis
     */
    private void validatePasswordNotCompromised(String password, String email) {
        log.debug("Checking password compromise for email: {}", email);

        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(password);

        if (decision.isCompromised()) {
            log.warn("Compromised password detected for email: {}", email);
            throw exceptionFactory.validationError(
                    "password",
                    getLocalizedMessage("validation.password.compromised")
            );
        }

        log.debug("Password check passed for email: {}", email);
    }

    /**
     * Vérifie qu'il n'existe pas déjà un utilisateur avec le même email ou mobile.
     *
     * @param request Données d'inscription
     * @throws com.store.store.exception.ValidationException si des doublons sont détectés
     */
    private void checkNoDuplicates(RegisterRequestDto request) {
        log.debug("Checking for duplicate email or mobile for: {}", request.getEmail());

        Optional<Customer> existingCustomer = customerRepository.findByEmailOrMobileNumber(
                request.getEmail(),
                request.getMobileNumber()
        );

        if (existingCustomer.isPresent()) {
            Map<String, String> errors = buildDuplicateErrors(existingCustomer.get(), request);
            log.warn("Duplicate registration attempt detected: {}", errors.keySet());
            throw exceptionFactory.validationError(errors);
        }

        log.debug("No duplicates found for: {}", request.getEmail());
    }

    /**
     * Construit un map d'erreurs détaillant les champs dupliqués.
     *
     * @param existingCustomer Customer existant en base
     * @param request Nouvelles données d'inscription
     * @return Map avec les champs en erreur
     */
    private Map<String, String> buildDuplicateErrors(Customer existingCustomer, RegisterRequestDto request) {
        Map<String, String> errors = new HashMap<>();

        if (existingCustomer.getEmail().equalsIgnoreCase(request.getEmail())) {
            errors.put("email", getLocalizedMessage("validation.email.already.exists"));
        }

        if (existingCustomer.getMobileNumber().equals(request.getMobileNumber())) {
            errors.put("mobileNumber", getLocalizedMessage("validation.mobileNumber.already.exists"));
        }

        return errors;
    }

    // MÉTHODES PRIVÉES - CRÉATION
    /**
     * Crée un nouveau Customer à partir des données d'inscription.
     *
     * @param request Données d'inscription
     * @return Customer initialisé (non sauvegardé)
     */
    private Customer createCustomer(RegisterRequestDto request) {
        log.debug("Creating new customer for email: {}", request.getEmail());

        Customer customer = new Customer();
        BeanUtils.copyProperties(request, customer);
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        log.debug("Customer created successfully");

        return customer;
    }

    /**
     * Assigne les rôles initiaux à un nouveau customer.
     * Par défaut : ROLE_USER
     *
     * @param customer Customer à qui assigner les rôles
     * @param request Données d'inscription (peut contenir des infos pour déterminer les rôles)
     */
    private void assignInitialRoles(Customer customer, RegisterRequestDto request) {
        log.debug("Assigning initial roles for: {}", request.getEmail());

        Set<Role> initialRoles = roleAssignmentService.determineInitialRoles(request);
        customer.setRoles(initialRoles);

        log.debug("Assigned {} roles to customer", initialRoles.size());
    }


    // MÉTHODES UTILITAIRES
    /**
     * Récupère un message localisé depuis le fichier messages.properties.
     *
     * @param code Code du message
     * @param args Arguments optionnels pour le message
     * @return Message traduit dans la locale courante
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}