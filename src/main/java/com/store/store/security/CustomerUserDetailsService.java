// src/main/java/com/store/store/security/CustomerUserDetailsService.java

package com.store.store.security;

import com.store.store.entity.Customer;
import com.store.store.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de chargement des détails utilisateur pour Spring Security.
 *
 * RESPONSABILITÉ:
 * Charger un Customer depuis la base de données et le convertir en UserDetails
 * pour que Spring Security puisse l'utiliser dans le processus d'authentification.
 *
 * UTILISÉ PAR:
 * - JwtAuthenticationFilter (pour valider le JWT et charger les détails user)
 * - DaoAuthenticationProvider (lors du login)
 * - SecurityConfig (injection dans authenticationProvider)
 *
 * @author Kardigué
 * @version 3.0 - Production Ready
 * @since 2025-01-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    /**
     * ✅ Charge les détails d'un utilisateur par son email (username).
     *
     * IMPORTANT:
     * - Charge les rôles en EAGER pour éviter LazyInitializationException
     * - Le contexte @Transactional est nécessaire pour charger les relations
     *
     * @param username Email de l'utilisateur
     * @return UserDetails contenant Customer et ses rôles
     * @throws UsernameNotFoundException Si l'utilisateur n'existe pas
     */
    @Override
    @Transactional(readOnly = true)  // ✅ IMPORTANT pour charger les rôles
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username: {}", username);

        // Charger le customer avec ses rôles (EAGER)
        Customer customer = customerRepository.findByEmailWithRoles(username)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", username);
                    return new UsernameNotFoundException("User not found with email: " + username);
                });

        log.debug("User found: {} with {} roles",
                customer.getEmail(),
                customer.getRoles() != null ? customer.getRoles().size() : 0);

        // Convertir Customer en UserDetails (record CustomerUserDetails)
        return new CustomerUserDetails(customer);
    }
}