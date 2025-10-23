package com.store.store.controller;

import com.store.store.dto.*;
import com.store.store.entity.Customer;
import com.store.store.entity.Role;
import com.store.store.repository.CustomerRepository;
import com.store.store.security.CustomerUserDetails;
import com.store.store.service.RoleAssignmentService;
import com.store.store.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.core.Authentication;

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
    private final PasswordEncoder passwordEncoder;
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    private final JwtUtil jwtUtil;
    private final RoleAssignmentService roleAssignmentService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> apiLogin(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        log.debug("Tentative de connexion pour l'utilisateur: {}", loginRequestDto.username());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.username(),
                        loginRequestDto.password()
                )
        );

        CustomerUserDetails userDetails = (CustomerUserDetails) authentication.getPrincipal();
        Customer loggedInUser = userDetails.customer();

        var userDto = new UserDto();
        BeanUtils.copyProperties(loggedInUser, userDto);

        userDto.setRoles(
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","))
        );

        if (loggedInUser.getAddress() != null) {
            var addressDto = new AddressDto();
            BeanUtils.copyProperties(loggedInUser.getAddress(), addressDto);
            userDto.setAddress(addressDto);
        }

        String jwtToken = jwtUtil.generateJwtToken(authentication);

        log.info("Connexion réussie pour l'utilisateur: {}", loggedInUser.getEmail());

        return ResponseEntity.ok(new LoginResponseDto("OK", userDto, jwtToken));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDto registerRequestDto) {
        log.debug("Tentative d'inscription pour l'email: {}", registerRequestDto.getEmail());

        // Vérifier si le mot de passe est compromis
        CompromisedPasswordDecision decision = compromisedPasswordChecker.check(registerRequestDto.getPassword());
        if (decision.isCompromised()) {
            log.warn("Mot de passe compromis détecté pour l'email: {}", registerRequestDto.getEmail());
            return ResponseEntity.badRequest().body(Map.of("password", "Choisissez un mot de passe fort"));
        }

        // Vérifier si l'email ou le numéro de téléphone existe déjà
        Optional<Customer> existingCustomer = customerRepository.findByEmailOrMobileNumber(
                registerRequestDto.getEmail(),
                registerRequestDto.getMobileNumber()
        );

        if (existingCustomer.isPresent()) {
            Map<String, String> errors = buildDuplicateErrors(existingCustomer.get(), registerRequestDto);
            log.warn("Tentative d'inscription avec des données existantes: {}", errors.keySet());
            return ResponseEntity.badRequest().body(errors);
        }

        // Créer le nouveau customer
        Customer customer = new Customer();
        BeanUtils.copyProperties(registerRequestDto, customer);
        customer.setPasswordHash(passwordEncoder.encode(registerRequestDto.getPassword()));

        Set<Role> initialRoles = roleAssignmentService.determineInitialRoles(registerRequestDto);
        customer.setRoles(initialRoles);

        customerRepository.save(customer);

        log.info("Nouvel utilisateur enregistré avec succès: {}", customer.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body("Inscription réussie");
    }

    private Map<String, String> buildDuplicateErrors(Customer existingCustomer, RegisterRequestDto request) {
        Map<String, String> errors = new HashMap<>();

        if (existingCustomer.getEmail().equalsIgnoreCase(request.getEmail())) {
            errors.put("email", "L'e-mail est déjà enregistré");
        }
        if (existingCustomer.getMobileNumber().equals(request.getMobileNumber())) {
            errors.put("mobileNumber", "Le numéro de téléphone portable est déjà enregistré");
        }

        return errors;
    }
}