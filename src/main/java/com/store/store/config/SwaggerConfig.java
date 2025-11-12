package com.store.store.config;

import com.store.store.constants.SwaggerConfigConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classe de configuration pour la documentation et les paramètres de sécurité OpenAPI.
 * Cette classe configure la documentation OpenAPI de l'application et inclut
 * les configurations de sécurité utilisant JWT (JSON Web Token). Elle fournit des métadonnées
 * telles que le titre, la description, la version, les coordonnées et les informations de licence de l'API.
 * Principales caractéristiques de cette configuration :
 * Définit les métadonnées de l'API, notamment le titre, la description, la version, les coordonnées et la licence.
 * Configure les schémas de sécurité pour l'authentification basée sur JWT à l'aide du mécanisme Bearer Token.
 * la configuration est utilisée par la spécification OpenAPI 3.0 pour générer la documentation de l'API,
 * qui sert d'interface permettant aux utilisateurs de comprendre et d'interagir avec les points de terminaison de l'API disponibles.
 * @author Kardigué
 *@version 1.1 - FIXED
 * @since 2025-10-27
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI storeOpenAPI() {

        return new OpenAPI()
                // Informations générales
                .info(new Info()
                        .title(SwaggerConfigConstants.API_TITLE)
                        .description(SwaggerConfigConstants.API_DESCRIPTION)
                        .version(SwaggerConfigConstants.API_VERSION)
                        .termsOfService(SwaggerConfigConstants.API_TERMS_OF_SERVICE_URL)
                        .contact(new Contact()
                                .name(SwaggerConfigConstants.CONTACT_NAME)
                                .email(SwaggerConfigConstants.CONTACT_EMAIL)
                                .url(SwaggerConfigConstants.CONTACT_URL))
                        .license(new License()
                                .name(SwaggerConfigConstants.LICENSE_NAME)
                                .url(SwaggerConfigConstants.LICENSE_URL))
                )

                // schéma de sécurité JWT
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SwaggerConfigConstants.BEARER_AUTHENTICATION,
                                new SecurityScheme()
                                        .name(SwaggerConfigConstants.BEARER_AUTHENTICATION)
                                        .description(SwaggerConfigConstants.BEARER_AUTHENTICATION_DESCRIPTION)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme(SwaggerConfigConstants.BEARER_AUTHENTICATION_SCHEME)
                                        .bearerFormat(SwaggerConfigConstants.BEARER_AUTHENTICATION_BEARER_FORMAT)
                        )
                )

                // Application du schéma à tous les endpoints sécurisés
                .addSecurityItem(new SecurityRequirement()
                        .addList(SwaggerConfigConstants.SECURITY_REFERENCE))

                // tag global
                .addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                        .name(SwaggerConfigConstants.API_TAG)
                        .description(SwaggerConfigConstants.API_TAG_DESCRIPTION));
    }
}