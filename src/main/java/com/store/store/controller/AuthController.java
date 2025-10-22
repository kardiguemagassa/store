package com.store.store.controller;

import com.store.store.dto.*;
import com.store.store.entity.Customer;
import com.store.store.repository.CustomerRepository;
import com.store.store.repository.RoleRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour gérer l'authentification et l'inscription des utilisateurs.
 * Fournit des endpoints pour le login et le register avec JWT.
 *
 * @author Kardigué
 * @version 1.0
 * @since 20-10-2025
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    private final JwtUtil jwtUtil;

    /**
     * Authentifie un utilisateur et génère un token JWT.
     *
     * @param loginRequestDto Les credentials de l'utilisateur (email et mot de passe)
     * @return ResponseEntity contenant les informations de l'utilisateur et le JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> apiLogin(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        try {
            log.debug("Tentative de connexion pour l'utilisateur: {}", loginRequestDto.username());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.username(),
                            loginRequestDto.password()
                    )
            );

            //Récupérer CustomerUserDetails puis extraire Customer
            CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
            Customer loggedInUser = userDetails.customer();

            // Mapper Customer vers UserDto en utilisant BeanUtils
            var userDto = new UserDto();
            BeanUtils.copyProperties(loggedInUser, userDto);

            // Mapper les rôles depuis les authorities
            userDto.setRoles(
                    authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","))
            );

            // Mapper l'adresse si présente
            if (loggedInUser.getAddress() != null) {
                var addressDto = new AddressDto();
                BeanUtils.copyProperties(loggedInUser.getAddress(), addressDto);
                userDto.setAddress(addressDto);
            }

            // Générer le JWT token
            String jwtToken = jwtUtil.generateJwtToken(authentication);

            log.info("Connexion réussie pour l'utilisateur: {}", loggedInUser.getEmail());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(new LoginResponseDto(
                            HttpStatus.OK.getReasonPhrase(),
                            userDto,
                            jwtToken
                    ));

        } catch (BadCredentialsException ex) {
            log.warn("Échec de connexion: credentials invalides pour {}", loginRequestDto.username());
            return buildErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    "Nom d'utilisateur ou mot de passe invalide"
            );
        } catch (AuthenticationException ex) {
            log.error("Erreur d'authentification pour {}: {}", loginRequestDto.username(), ex.getMessage());
            return buildErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    "L'authentification a échoué"
            );
        } catch (Exception ex) {
            log.error("Erreur inattendue lors de la connexion", ex);
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Une erreur inattendue s'est produite"
            );
        }
    }

    /**
     * Enregistre un nouvel utilisateur dans le système.
     *
     * @param registerRequestDto Les informations de l'utilisateur à enregistrer
     * @return ResponseEntity avec un message de succès ou d'erreur
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequestDto) {

        log.debug("Tentative d'inscription pour l'email: {}", registerRequestDto.getEmail());

        // Vérifier si le mot de passe est compromis
        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(registerRequestDto.getPassword());
        if (decision.isCompromised()) {
            log.warn("Mot de passe compromis détecté pour l'email: {}", registerRequestDto.getEmail());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("password", "Choisissez un mot de passe fort"));
        }

        // Vérifier si l'email ou le numéro de téléphone existe déjà
        Optional<Customer> existingCustomer = customerRepository.findByEmailOrMobileNumber(
                registerRequestDto.getEmail(),
                registerRequestDto.getMobileNumber()
        );

        if (existingCustomer.isPresent()) {
            Map<String, String> errors = new HashMap<>();
            Customer customer = existingCustomer.get();

            if (customer.getEmail().equalsIgnoreCase(registerRequestDto.getEmail())) {
                errors.put("email", "L'e-mail est déjà enregistré");
            }
            if (customer.getMobileNumber().equals(registerRequestDto.getMobileNumber())) {
                errors.put("mobileNumber", "Le numéro de téléphone portable est déjà enregistré");
            }

            log.warn("Tentative d'inscription avec des données existantes: {}", errors.keySet());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        // Créer le nouveau customer
        Customer customer = new Customer();
        BeanUtils.copyProperties(registerRequestDto, customer);
        customer.setPasswordHash(passwordEncoder.encode(registerRequestDto.getPassword()));

        // Assigner le rôle USER par défaut
        roleRepository.findByName("ROLE_USER")
                .ifPresent(role -> customer.setRoles(Set.of(role)));

        customerRepository.save(customer);

        log.info("Nouvel utilisateur enregistré avec succès: {}", customer.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Inscription réussie");
    }

    /**
     * Méthode utilitaire pour construire une réponse d'erreur standardisée.
     *
     * @param status Le statut HTTP de l'erreur
     * @param message Le message d'erreur
     * @return ResponseEntity avec les détails de l'erreur
     */
    private ResponseEntity<LoginResponseDto> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new LoginResponseDto(message, null, null));
    }
}