package com.store.store.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre pour exposer le token CSRF dans les headers de réponse.
 * Force la création du cookie XSRF-TOKEN lors de chaque requête.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-14
 */
@Slf4j
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            // CRITIQUE : Force la création du cookie XSRF-TOKEN
            String token = csrfToken.getToken();

            // Exposer le token dans un header
            response.setHeader("X-CSRF-TOKEN", token);

            log.debug("CSRF token exposed for path: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}