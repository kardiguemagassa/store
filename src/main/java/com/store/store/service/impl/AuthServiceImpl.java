package com.store.store.service.impl;

import com.store.store.dto.LoginRequestDto;
import com.store.store.dto.LoginResponseDto;
import com.store.store.dto.RegisterRequestDto;
import com.store.store.dto.UserDto;

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

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


import static com.store.store.constants.TokenConstants.ACCESS_TOKEN_EXPIRY_SECONDS;

/**

 * Authentication service managing user registration and login.

 * Responsibilities:
 * - Registration of new users with full validation
 * - User login with JWT + Refresh Token generation
 * Verification of compromised passwords
 * Management of duplicates (email, mobile)

 * @author Kardigué
 * @version 3.0 (JWT + Refresh Token + Cookies)
 * @since 2025-11-01
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
     * Registers a new user in the system by performing a series of validation and setup steps:
     * 1. Validates that the password is not compromised.
     * 2. Ensures there are no duplicate users based on email or mobile number.
     * 3. Creates a new customer entity.
     * 4. Assigns initial roles to the new customer.
     * 5. Persists the customer into the database.
     *
     * @param request The registration request data, including user's name, email, mobile number, and password.
     *                The request must provide valid and non-blank details for these fields.
     * @throws com.store.store.exception.ValidationException If the password is compromised.
     * @throws com.store.store.exception.ValidationException If a user with the same email or mobile number already exists.
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
     * Authenticates a user based on provided login credentials and additional client information,
     * and returns a response containing access tokens and user details.
     *
     * The method performs the following steps:
     * 1. Authenticates the user using Spring Security.
     * 2. Retrieves the authenticated customer's details.
     * 3. Constructs a UserDto with user and authentication details.
     * 4. Generates a short-lived JWT token for session management.
     * 5. Creates and stores a long-lived refresh token.
     * 6. Builds and returns the login response with all relevant data.
     *
     * @param request The login request containing the user's email (username) and password for authentication.
     * @param ipAddress The IP address of the client attempting the login.
     * @param userAgent The user agent information of the client's device.
     * @return A {@link LoginResponseDto} object that provides the login status, user information,
     *         JWT access token, refresh token, and token expiry information.
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

        log.info("Refresh token created and stored in database for user: {} from IP: {}",
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
     * Validates that the provided password has not been previously compromised.
     *
     * This method checks the password against a compromised password database
     * to ensure it is secure and not already known to be vulnerable. If the password
     * is found to be compromised, an exception is thrown.
     *
     * @param password The password to be validated.
     * @param email The email associated with the password, used for logging and traceability.
     * @throws com.store.store.exception.ValidationException If the password is detected as compromised.
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
     * Checks if there are any duplicate users in the system based on the email or mobile number
     * provided in the registration request. If duplicates are found, a validation error is thrown.
     *
     * @param request The registration request containing user details such as email and mobile number,
     *                which are used to check for duplicates.
     * @throws com.store.store.exception.ValidationException If a user with the same email or mobile number already exists.
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
     * Builds a map of duplicate error messages for a given customer based on email and mobile number.
     *
     * The method compares the provided customer's email and mobile number with the ones in the
     * registration request. If duplicates are found, corresponding localized error messages are added
     * to the map.
     *
     * @param existingCustomer The existing customer whose details will be checked for conflicts.
     * @param request The registration request containing the email and mobile number to check
     *                against the existing customer's information.
     * @return A map containing error messages for duplicate fields. The keys represent the field names
     *         (e.g., "email", "mobileNumber") and the values are the corresponding localized error
     *         messages (e.g., email or mobile number already exists).
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
     * Creates a new Customer instance based on the provided RegisterRequestDto.
     * Copies properties from the request to the Customer entity,
     * hashes the password provided in the request, and populates the password hash in the Customer object.
     *
     * @param request The registration request data containing user details such as name, email, mobile number, and password.
     *                The request must provide valid and non-blank details for these fields.
     * @return A Customer object populated with the data from the registration request, including the hashed password.
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
     * Assigns initial roles to a newly created customer based on the registration request information.
     *
     * This method determines the appropriate roles for the customer using the role assignment
     * service and sets these roles on the customer entity. It is used as part of the user
     * registration process to ensure that the customer has the correct initial roles for the system.
     *
     * @param customer The newly created {@link Customer} entity to which roles will be assigned.
     * @param request The registration request data that provides context for determining the initial roles,
     *                such as the customer's email, name, or other relevant data.
     */
    private void assignInitialRoles(Customer customer, RegisterRequestDto request) {
        log.debug("Assigning initial roles for: {}", request.getEmail());

        Set<Role> initialRoles = roleAssignmentService.determineInitialRoles(request);
        customer.setRoles(initialRoles);

        log.debug("Assigned {} roles to customer", initialRoles.size());
    }


    // MÉTHODES UTILITAIRES

    /**
     * Retrieves a localized message for the specified message code and arguments,
     * based on the current locale.
     *
     * The localization is performed using the provided code and optional arguments,
     * which are formatted into the message retrieved from the underlying message source.
     *
     * @param code The code identifying the message in the message source.
     * @param args Optional arguments used to replace placeholders in the localized message.
     * @return The localized message as a string, formatted with the given arguments.
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}