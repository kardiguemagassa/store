package com.store.store.config;

import com.store.store.security.CustomerUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.security.Principal;
import java.util.Optional;

/**
 * Implémentation de l'interface {@link AuditorAware} pour déterminer l'auditeur
 * actuel à des fins d'audit.
 * Cette classe récupère l'identité de l'auditeur actuel en fonction du contexte de sécurité,
 * évaluant différents états d'authentification et utilisant les valeurs par défaut en cas d'erreur ou
 * d'absence d'informations d'authentification.
 * Les scénarios suivants sont gérés pour identifier l'auditeur actuel :
 * Si aucune authentification n'est disponible, l'utilisateur système est utilisé comme auditeur.
 * Si l'authentification n'est pas valide ou si l'utilisateur authentifié est anonyme,
 * l'utilisateur système est utilisé comme auditeur.
 * Si le principal authentifié est de type {@code CustomerUserDetails},
 * l'adresse e-mail de l'utilisateur est utilisée comme auditeur.
 * Si le principal est une chaîne de caractères, cette chaîne est utilisée comme auditeur.
 * En dernier recours, le nom de l'objet d'authentification est utilisé comme auditeur.
 * En cas d'échec ou d'erreur inattendue, l'utilisateur système est renvoyé par défaut comme auditeur.
 * @auteur Kardigué
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