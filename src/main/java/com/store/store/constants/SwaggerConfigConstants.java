package com.store.store.constants;

import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfigConstants {

    public static final String SECURITY_REFERENCE = "Token Access";
    public static final String CONTACT_EMAIL = "magassakara@gmail.com";
    public static final String CONTACT_URL = "https://www.store.com";
    public static final String CONTACT_NAME = "Kardigué";
    public static final String API_TITLE = "Store Management open API";
    public static final String API_DESCRIPTION = """
        Store API est une API REST sécurisée permettant la gestion complète d’une boutique en ligne.
        Elle propose des endpoints pour :
        - l'authentification et la gestion des utilisateurs,
        - la gestion des produits, catégories et commandes,
        - le traitement des messages clients.

        L'API applique les bonnes pratiques REST, utilise une architecture en couches
        (Controller, Service, Repository) et est conçue pour être intégrée facilement
        avec des applications frontend modernes (Angular, React, Vue).
        """;

    public static final String API_VERSION = "1.0.0";
    public static final String API_TERMS_OF_SERVICE_URL = "https://www.store.com/terms";
    public static final String LICENSE_NAME ="MIT";
    public static final String LICENSE_URL = "https://opensource.org/licenses/MIT";
    public static final String API_TAG = "Store Management API";


    public static final String BEARER_AUTHENTICATION = "Bearer Authentication";
    public static final String BEARER_AUTHENTICATION_DESCRIPTION = "Authentication with JWT token";
    public static final String BEARER_AUTHENTICATION_SCHEME = "bearer";
    public static final String BEARER_AUTHENTICATION_BEARER_FORMAT = "JWT";

    public static final String API_TAG_DESCRIPTION = "API centralisée de gestion de la boutique";

}
