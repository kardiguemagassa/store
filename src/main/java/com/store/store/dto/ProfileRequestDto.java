package com.store.store.dto;

import jakarta.validation.constraints.*;
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

    @Size(min = 5, max = 50, message = "Le nom de la rue doit être compris entre 5 et 50 caractères")
    private String street;

    @Size(min = 3, max = 30, message = "Le nom de la ville doit être compris entre 3 et 30 caractères")
    private String city;

    @Size(min = 2, max = 30, message = "Le nom du département doit être compris entre 2 et 30 caractères")
    private String state;

    @Pattern(regexp = "^\\d{5}$", message = "Le code postal doit comporter exactement 5 chiffres")
    private String postalCode;

    @Size(min = 2, max = 2, message = "La nom du pays doit être exactement de 2 caractères")
    private String country;

    // Validation au niveau DTO
    @AssertTrue(message = "L'adresse doit être complète (tous les champs) ou entièrement absente")
    public boolean isAddressComplete() {
        boolean hasStreet = street != null && !street.trim().isEmpty();
        boolean hasCity = city != null && !city.trim().isEmpty();
        boolean hasState = state != null && !state.trim().isEmpty();
        boolean hasPostalCode = postalCode != null && !postalCode.trim().isEmpty();
        boolean hasCountry = country != null && !country.trim().isEmpty();

        // Compter les champs remplis
        int filledCount = 0;
        if (hasStreet) filledCount++;
        if (hasCity) filledCount++;
        if (hasState) filledCount++;
        if (hasPostalCode) filledCount++;
        if (hasCountry) filledCount++;

        // Soit 0 pas d'adresse, soit 5 adresse complète
        return filledCount == 0 || filledCount == 5;
    }
}