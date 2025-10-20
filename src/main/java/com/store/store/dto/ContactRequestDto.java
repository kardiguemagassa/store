package com.store.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestDto {

    @NotBlank(message = "Le nom ne peut pas être vide")
    @Size(min =5, max = 30, message = "Le nom doit comporter entre 5 et 30 caractères")
    private String name;

    @NotBlank(message = "L'e-mail ne peut pas être vide")
    @Email(message = "Adresse e-mail invalide")
    private String email;

    @NotBlank(message = "Le numéro de téléphone portable ne peut pas être vide")
    @Pattern(regexp = "^\\d{10}$", message = "Le numéro de téléphone portable doit comporter 10 chiffres")
    private String mobileNumber;

    @NotBlank(message = "Le message ne peut pas être vide")
    @Size(min =5, max = 500, message = "Le message doit comporter entre 5 et 500 caractères")
    private String message;

}

