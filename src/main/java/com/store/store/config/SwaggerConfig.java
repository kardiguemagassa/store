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
 * Configuration class for OpenAPI documentation and security settings.
 *
 * This class sets up the OpenAPI documentation for the application and includes
 * security configurations using JWT (JSON Web Token). It provides metadata
 * such as the API's title, description, version, contact details, and licensing
 * information.
 *
 * Key features of this configuration:
 * - Defines API metadata including title, description, version, contact, and license.
 * - Configures security schemes for JWT-based authentication using the Bearer Token mechanism.
 *
 * The configuration is used by the OpenAPI 3.0 specification to generate API
 * documentation, which serves as an interface for users to understand and
 * interact with the available API endpoints.
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