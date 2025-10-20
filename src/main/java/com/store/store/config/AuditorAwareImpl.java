package com.store.store.config;

import com.store.store.security.CustomerUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Slf4j
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final String ANONYMOUS_USER = "anonymousUser";


    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Pas d'authentification
            if (authentication == null) {
                log.debug("No authentication found, using system as auditor");
                return Optional.of(SYSTEM_USER);
            }

            // Utilisateur non authentifié
            if (!authentication.isAuthenticated()) {
                log.debug("User not authenticated, using system as auditor");
                return Optional.of(SYSTEM_USER);
            }

            Object principal = authentication.getPrincipal();

            // Utilisateur anonyme
            if (ANONYMOUS_USER.equals(principal)) {
                log.debug("Anonymous user detected, using SYSTEM as auditor");
                return Optional.of(SYSTEM_USER);
            }

            // CustomerUserDetails
            if (principal instanceof CustomerUserDetails customerUserDetails) {
                String email = customerUserDetails.getUsername();
                log.debug("Auditor identified as: {}", email);
                return Optional.of(email);
            }

            // Fallback : String username
            if (principal instanceof String username) {
                log.debug("String principal detected, using as auditor: {}", username);
                return Optional.of(username);
            }

            // Dernier recours
            String name = authentication.getName();
            log.debug("Using authentication name as auditor: {}", name);
            return Optional.of(name);

        } catch (Exception e) {
            log.error("Error getting current auditor, falling back to SYSTEM", e);
            return Optional.of(SYSTEM_USER);
        }
    }

    /*
    // Version AVANCÉE (avec CustomerUserDetails)
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.of("SYSTEM");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomerUserDetails customerUserDetails) {
            return Optional.of(customerUserDetails.getUsername());
        }

        return Optional.of(authentication.getName());
    }
     */
}