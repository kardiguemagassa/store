package com.store.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequestDto {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 30, message = "Le nom doit être compris entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "Email is obligatoire")
    @Email(message = "L'adresse e-mail doit être une valeur valide")
    private String email;

    @NotBlank(message = "Le numéro de téléphone portable ne peut pas être vide")
    @Pattern(regexp = "^\\d{10}$", message = "Le numéro de téléphone portable doit comporter 10 chiffres")
    private String mobileNumber;

    @NotBlank(message = "La rue est obligatoire")
    @Size(min = 5, max = 50, message = "Le nom de la rue doit être compris entre 5 et 50 caractères")
    private String street;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 3, max = 30, message = "Le nom de la ville doit être compris entre 3 et 30 caractères")
    private String city;

    @NotBlank(message = "Le département est obligatoire")
    @Size(min = 2, max = 30, message = "Le nom du département doit être compris entre 2 et 30 caractères")
    private String state;

    @NotBlank(message = "Le code postal est obligatoire")
    @Pattern(regexp = "^\\d{5}$", message = "Le code postal doit comporter exactement 5 chiffres")
    private String postalCode;

    @NotBlank(message = "Le pays est obligatoire")
    @Size(min = 2, max = 2, message = "La nom du pays doit être exactement de 2 caractères")
    private String country;

}
