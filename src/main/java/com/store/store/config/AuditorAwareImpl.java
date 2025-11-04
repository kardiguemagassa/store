package com.store.store.config;

import com.store.store.security.CustomerUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

/**
 * Implementation of the {@link AuditorAware} interface for determining the
 * current auditor for auditing purposes.
 *
 * This class retrieves the current auditor's identity based on the security
 * context, evaluating different authentication states and falling back to
 * default values in case of errors or absence of authentication details.
 *
 * The following scenarios are handled to identify the current auditor:
 *
 * - If no authentication is available, the system user is used as the auditor.
 * - If the authentication is not valid or the authenticated user is anonymous,
 *   the system user is used as the auditor.
 * - If the authenticated principal is of type {@code CustomerUserDetails}, the
 *   email of the user is used as the auditor.
 * - If the principal is a plain string, the string value is used as the auditor.
 * - As a fallback, the name from the authentication object is used as the auditor.
 *
 * On failure or unexpected errors, the system user is returned by default as the auditor.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Slf4j
public class AuditorAwareImpl implements AuditorAware<String> {

    private static final String SYSTEM_USER = "system";
    private static final String ANONYMOUS_USER = "anonymousUser";

    @Override
    public Optional<String> getCurrentAuditor() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                log.trace("No authentication context available");
                return Optional.of(SYSTEM_USER);
            }

            Object principal = authentication.getPrincipal();

            if (ANONYMOUS_USER.equals(principal)) {
                log.trace("Anonymous user detected");
                return Optional.of(SYSTEM_USER);
            }

            String username = extractUsername(principal);
            log.debug("Current auditor identified: {}", username);

            // S'assurer que le username n'est pas null/vide
            if (username == null || username.trim().isEmpty()) {
                log.warn("Extracted username is null or empty, using system user");
                return Optional.of(SYSTEM_USER);
            }

            return Optional.of(username);

        } catch (Exception e) {
            log.warn("Error determining current auditor, falling back to system user", e);
            return Optional.of(SYSTEM_USER);
        }
    }

    private String extractUsername(Object principal) {
        // Utilisation de instanceof classique au lieu du pattern matching
        if (principal instanceof CustomerUserDetails) {
            return ((CustomerUserDetails) principal).getUsername();
        } else if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        } else if (principal instanceof Principal) {
            return ((Principal) principal).getName();
        } else {
            log.warn("Unexpected principal type: {}", principal.getClass().getName());
            return principal.toString();
        }
    }
}