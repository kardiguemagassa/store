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
 * Service de chargement des {@link UserDetails} à partir de l'adresse e-mail d'un utilisateur.
 * Cette classe implémente {@link UserDetailsService} pour fournir
 * une passerelle permettant d'intégrer des détails utilisateur personnalisés à Spring Security.
 * Cette implémentation gère spécifiquement le chargement des entités {@code Customer}, ainsi que leurs rôles associés,
 * afin de créer un objet {@code UserDetails} qui s'intègre au système d'authentification.
 * Caractéristiques:
 * - Utilise {@link Transactional} pour gérer le chargement différé
 * des relations et garantir l'initialisation correcte des rôles associés.
 * - Charge les rôles de manière immédiate afin d'éviter une {@link org.hibernate.LazyInitializationException}.
 * - Gère la recherche d'utilisateurs par leur adresse e-mail (nom d'utilisateur).
 * @author Kardigué
 * @version 3.0 - Prêt pour la production
 * @since 2025-10-27

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    /**
     * Charge les informations de l'utilisateur à partir du nom d'utilisateur fourni, qui représente son adresse e-mail.
     * Cette méthode récupère une entité {@code Customer} de la base de données ainsi que tous les rôles associés.
     * La méthode convertit l'entité {@code Customer} en un objet {@code UserDetails} pour l'intégration avec
     * le framework Spring Security.
     * @param username l'adresse e-mail de l'utilisateur à charger.
     * @return un objet {@code UserDetails} contenant les informations et les rôles de l'utilisateur.
     * @throws UsernameNotFoundException si aucun utilisateur n'est trouvé avec l'adresse e-mail spécifiée.
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