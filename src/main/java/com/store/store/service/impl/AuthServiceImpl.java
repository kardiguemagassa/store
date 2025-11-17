package com.store.store.service.impl;

import com.store.store.dto.auth.LoginRequestDto;
import com.store.store.dto.auth.LoginResponseDto;
import com.store.store.dto.auth.RegisterRequestDto;
import com.store.store.dto.user.UserDto;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.store.store.constants.TokenConstants.ACCESS_TOKEN_EXPIRY_SECONDS;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-06
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
    private final MessageServiceImpl messageService;
    private final UserMapper userMapper;

    // INSCRIPTION (REGISTER)

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

    // CONNEXION (LOGIN)

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent) {
        log.debug("Login attempt for username: {} from IP: {}", request.username(), ipAddress);

        // 1. Authentification Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        // 2. Récupérer le customer authentifié
        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Customer loggedInUser = userDetails.customer();

        // 3. Construire le UserDto avec UserMapper
        UserDto userDto = userMapper.toUserDto(loggedInUser, authentication);

        // 4. Générer Access Token (JWT 15 min)
        String jwtToken = jwtUtil.generateJwtToken(authentication);
        log.info("Access token generated for user: {}", loggedInUser.getEmail());

        // 5. Créer Refresh Token (UUID 7 jours)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(loggedInUser, ipAddress, userAgent);

        log.info("Refresh token created and stored in database for user: {} from IP: {}",
                loggedInUser.getEmail(), ipAddress);

        // 6. Construire la réponse
        log.info("Login successful for user: {} from IP: {}", loggedInUser.getEmail(), ipAddress);

        return new LoginResponseDto(
                "Login successful",              // message (statique, sera géré par le Controller)
                userDto,                          // user
                jwtToken,                         // jwtToken
                refreshToken.getToken(),          // refreshToken (UUID)
                ACCESS_TOKEN_EXPIRY_SECONDS       // expireIn (900 secondes = 15 min)
        );
    }

    // VALIDATION - MOT DE PASSE

    private void validatePasswordNotCompromised(String password, String email) {
        log.debug("Checking password compromise for email: {}", email);

        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(password);

        if (decision.isCompromised()) {
            log.warn("Compromised password detected for email: {}", email);
            // Utilisation de messageService
            throw exceptionFactory.validationError("password", messageService.getMessage("validation.password.compromised"));
        }

        log.debug("Password check passed for email: {}", email);
    }

    // VALIDATION - DOUBLONS

    private void checkNoDuplicates(RegisterRequestDto request) {
        log.debug("Checking for duplicate email or mobile for: {}", request.getEmail());

        Optional<Customer> existingCustomer = customerRepository.findByEmailOrMobileNumber(
                request.getEmail(),
                request.getMobileNumber()
        );

        if (existingCustomer.isPresent()) {
            Map<String, String> errors = buildDuplicateErrors(existingCustomer.get(), request);
            log.warn("Duplicate registration attempt detected: {}", errors.keySet());
            // Exception avec Map d'erreurs
            throw exceptionFactory.validationError(errors);
        }

        log.debug("No duplicates found for: {}", request.getEmail());
    }

    private Map<String, String> buildDuplicateErrors(Customer existingCustomer, RegisterRequestDto request) {
        Map<String, String> errors = new HashMap<>();

        if (existingCustomer.getEmail().equalsIgnoreCase(request.getEmail())) {
            // Utilisation de messageService
            errors.put("email", messageService.getMessage("validation.email.already.exists"));
        }

        if (existingCustomer.getMobileNumber().equals(request.getMobileNumber())) {
            // Utilisation de messageService
            errors.put("mobileNumber", messageService.getMessage("validation.mobileNumber.already.exists"));
        }

        return errors;
    }

    // CRÉATION - CUSTOMER

    private Customer createCustomer(RegisterRequestDto request) {
        log.debug("Creating new customer for email: {}", request.getEmail());

        Customer customer = new Customer();
        BeanUtils.copyProperties(request, customer);

        // Hashage sécurisé du mot de passe
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        log.debug("Customer created successfully");
        return customer;
    }

    // ATTRIBUTION - RÔLES

    private void assignInitialRoles(Customer customer, RegisterRequestDto request) {
        log.debug("Assigning initial roles for: {}", request.getEmail());

        Set<Role> initialRoles = roleAssignmentService.determineInitialRoles(request);
        customer.setRoles(initialRoles);

        log.debug("Assigned {} roles to customer", initialRoles.size());
    }

}