package com.store.store.security;

import com.store.store.entity.Customer;
import com.store.store.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Profile("!prod")
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreNonProdUsernamePwdAuthenticationProvider implements AuthenticationProvider {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        log.info("Tentative d'authentification pour: {}", email);

        try {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        log.warn("Utilisateur non trouvé: {}", email);
                        return new BadCredentialsException("Invalid credentials");
                    });

            log.info("Utilisateur trouvé: {} (ID: {})", customer.getEmail(), customer.getCustomerId());

            // Vérification du mot de passe avec logging détaillé
            if (!passwordEncoder.matches(password, customer.getPasswordHash())) {
                log.warn("Mot de passe incorrect pour: {}", email);
                throw new BadCredentialsException("Invalid credentials");
            }

            log.info("Mot de passe validé pour: {}", email);

            // Forcer le chargement des rôles LAZY
            if (customer.getRoles() != null) {
                log.info("Rôles chargés: {}", customer.getRoles().size());
                customer.getRoles().forEach(role ->
                        log.info("   - Role: {}", role.getName())
                );
            } else {
                log.warn("Aucun rôle trouvé pour: {}", email);
            }

            // Créer UserDetails avec validation
            CustomerUserDetails userDetails = new CustomerUserDetails(customer);

            log.info("Authentication créée avec succès pour: {}", email);

            return new UsernamePasswordAuthenticationToken(
                    userDetails,           // Principal = CustomerUserDetails
                    null,                 // Password = null pour sécurité
                    userDetails.getAuthorities()
            );

        } catch (BadCredentialsException e) {
            log.error("Erreur d'authentification pour {}: {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'authentification: {}", e.getMessage(), e);
            throw new BadCredentialsException("Authentication failed");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
