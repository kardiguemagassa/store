package com.store.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {
    @NotBlank(message = "Nom est obligatore")
    @Size(min = 2, max = 30, message = "La longueur du nom doit être comprise entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "Email est obligatore")
    @Email(message = "L'adresse e-mail doit être une valeur valide")
    private String email;

    @NotBlank(message = "Le numéro de téléphone portable est obligatore")
    @Pattern(regexp = "^\\d{10}$", message = "Le numéro de téléphone portable doit comporter exactement 10 chiffres")
    private String mobileNumber;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, max = 20, message = "Le mot de passe doit contenir entre 8 et 50 caractères")
    private String password;
}
