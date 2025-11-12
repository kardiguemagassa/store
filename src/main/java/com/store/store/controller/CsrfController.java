package com.store.store.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@RestController
@RequestMapping("/api/v1/csrf-token")
public class CsrfController {

    /**
     * Récupère le jeton CSRF associé à la requête actuelle.
     * @param request l'objet HttpServletRequest représentant la requête HTTP actuelle
     * @return le jeton CsrfToken associé à la requête, ou null si aucun jeton n'est disponible
     */
    @GetMapping
    public CsrfToken csrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
}