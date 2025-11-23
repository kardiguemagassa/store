package com.store.store.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@RestController
@RequestMapping("/api/v1/csrf-token")
public class CsrfController {

    /**
     * Endpoint dédié pour initialiser la session CSRF
     * Doit être appelé une fois au chargement de l'application
     */
    @GetMapping
    public ResponseEntity<CsrfResponse> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken == null) {
            return ResponseEntity.notFound().build();
        }

        CsrfResponse response = CsrfResponse.builder()
                .token(csrfToken.getToken())
                .headerName(csrfToken.getHeaderName())
                .parameterName(csrfToken.getParameterName())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.ok()
                .header(csrfToken.getHeaderName(), csrfToken.getToken())
                .body(response);
    }

    @Data
    @Builder
    public static class CsrfResponse {
        private String token;
        private String headerName;
        private String parameterName;
        private Instant timestamp;
    }
}