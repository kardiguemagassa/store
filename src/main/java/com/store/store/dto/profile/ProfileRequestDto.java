package com.store.store.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileRequestDto {

    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 100, message = "{validation.size.min.max}")
    private String name;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 150, message = "{validation.size.max}")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^\\d{10}$", message = "{validation.mobileNumber.pattern}")
    private String mobileNumber;

    @Size(min = 5, max = 100, message = "{validation.size.min.max}")
    private String street;

    @Size(min = 3, max = 50, message = "{validation.size.min.max}")
    private String city;

    @Size(min = 2, max = 50, message = "{validation.size.min.max}")
    private String state;

    @Pattern(regexp = "^\\d{5}$", message = "{validation.postalCode.pattern}")
    private String postalCode;

    @Size(min = 2, max = 2, message = "{validation.profile.country.size}")
    private String country;

    // Validation au niveau DTO
    @AssertTrue(message = "{validation.profile.address.complete}")
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


    // MÉTHODES UTILITAIRES
    /**
     * Vérifie si une adresse est fournie
     */
    public boolean hasAddress() {
        return (street != null && !street.trim().isEmpty()) ||
                (city != null && !city.trim().isEmpty()) ||
                (state != null && !state.trim().isEmpty()) ||
                (postalCode != null && !postalCode.trim().isEmpty()) ||
                (country != null && !country.trim().isEmpty());
    }

    /**
     * Vérifie si l'adresse est complète
     */
    public boolean isFullAddressProvided() {
        return street != null && !street.trim().isEmpty() &&
                city != null && !city.trim().isEmpty() &&
                state != null && !state.trim().isEmpty() &&
                postalCode != null && !postalCode.trim().isEmpty() &&
                country != null && !country.trim().isEmpty();
    }

    /**
     * Vérifie si aucun champ d'adresse n'est fourni
     */
    public boolean isAddressEmpty() {
        return (street == null || street.trim().isEmpty()) &&
                (city == null || city.trim().isEmpty()) &&
                (state == null || state.trim().isEmpty()) &&
                (postalCode == null || postalCode.trim().isEmpty()) &&
                (country == null || country.trim().isEmpty());
    }

    /**
     * Retourne une représentation textuelle de l'adresse
     */
    public String getFormattedAddress() {
        if (!isFullAddressProvided()) {
            return null;
        }

        StringBuilder address = new StringBuilder();
        address.append(street.trim());

        if (city != null && !city.trim().isEmpty()) {
            address.append(", ").append(city.trim());
        }

        if (postalCode != null && !postalCode.trim().isEmpty()) {
            address.append(" ").append(postalCode.trim());
        }

        if (state != null && !state.trim().isEmpty()) {
            address.append(", ").append(state.trim());
        }

        if (country != null && !country.trim().isEmpty()) {
            address.append(", ").append(country.trim().toUpperCase());
        }

        return address.toString();
    }
}