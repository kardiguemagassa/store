package com.store.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("contact")
public record ContactInfoDto(
        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        String phone,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String email,

        @NotBlank(message = "L'adresse est obligatoire")
        String address
) {}

