package com.store.store.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CsrfController is a REST controller that provides an endpoint for retrieving a CSRF token.
 * This token can be used to enhance security by preventing Cross-Site Request Forgery (CSRF) attacks.
 *
 * The controller responds to GET requests mapped to "/api/v1/csrf-token".
 *
 * @author Kardigué
 *  * @version 3.0
 *  * @since 2025-11-01
 */
@RestController
@RequestMapping("/api/v1/csrf-token")
public class CsrfController {

    /**
     * Retrieves the CSRF token associated with the current request.
     *
     * @param request the HttpServletRequest object representing the current HTTP request
     * @return the CsrfToken associated with the request, or null if no token is available
     */
    @GetMapping
    public CsrfToken csrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
}