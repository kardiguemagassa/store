package com.store.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        String username,
        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {
}

