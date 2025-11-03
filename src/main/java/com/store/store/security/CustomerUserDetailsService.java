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
 * Service for loading {@link UserDetails} based on a user's email.
 * This class implements {@link UserDetailsService} to provide
 * a bridge for integrating custom user details into Spring Security.
 *
 * This implementation specifically handles loading {@code Customer} entities, along with their associated roles,
 * to create a {@code UserDetails} object that integrates with the authentication system.
 *
 * Characteristics:
 * - Uses {@link Transactional} to manage lazy-loaded
 *   relations and ensure proper initialization of associated roles.
 * - Loads roles eagerly to avoid {@link org.hibernate.LazyInitializationException}.
 * - Handles user lookup by their email address (username).
 *
 * @author Kardigué
 * @version 3.0 - Production Ready
 * @since 2025-10-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    /**
     * Loads user details based on the provided username, which represents the user's email.
     * This method retrieves a {@code Customer} entity from the database along with all associated roles.
     * The method converts the {@code Customer} entity to a {@code UserDetails} object for integration with
     * the Spring Security framework.
     *
     * @param username the email address of the user to be loaded.
     * @return a {@code UserDetails} object containing the user's information and roles.
     * @throws UsernameNotFoundException if no user is found with the specified email address.
     */
    @Override
    @Transactional(readOnly = true)
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