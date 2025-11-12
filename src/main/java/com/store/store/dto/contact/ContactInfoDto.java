package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * DTO des informations de contact de l'entreprise.
 *
 * Chargé automatiquement depuis application.yml/properties :
 * <pre>
 * contact:
 *   phone: "+33 1 23 45 67 89"
 *   email: "contact@eazystore.com"
 *   address: "123 Rue de la Paix, 75001 Paris"
 * </pre>
 *
 * @author Kardigué
 * @version 1.0
 */
@Validated
@ConfigurationProperties("contact")
@Schema(description = "Informations de contact de l'entreprise")
public record ContactInfoDto(
        @NotBlank(message = "{validation.required}")
        @Size(max = 20, message = "{validation.size.max}")
        @Schema(description = "Numéro de téléphone", example = "+33 1 23 45 67 89")
        String phone,

        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.email}")
        @Size(max = 100, message = "{validation.size.max}")
        @Schema(description = "Adresse email", example = "contact@eazystore.com")
        String email,

        @NotBlank(message = "{validation.required}")
        @Size(max = 500, message = "{validation.size.max}")
        @Schema(description = "Adresse physique", example = "123 Rue de la Paix, 75001 Paris, France")
        String address
) {}