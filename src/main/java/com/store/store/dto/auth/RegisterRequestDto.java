package com.store.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Requête d'inscription d'un nouvel utilisateur")
public class RegisterRequestDto {

    @Schema(
            description = "Nom complet de l'utilisateur",
            example = "Jean Dupont",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 100, message = "{validation.size.min.max}")
    private String name;

    @Schema(
            description = "Adresse email valide",
            example = "jean.dupont@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 150, message = "{validation.size.max}")
    private String email;

    @Schema(
            description = "Numéro de mobile français (10 chiffres)",
            example = "0612345678",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^\\d{10}$", message = "{validation.mobileNumber.pattern}")
    private String mobileNumber;

    @Schema(
            description = "Mot de passe sécurisé",
            example = "MonMotDePasse123!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 8,
            maxLength = 128
    )
    @NotBlank(message = "{validation.required}")
    @Size(min = 8, max = 128, message = "{validation.register.password.size}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "{validation.register.password.pattern}"
    )
    private String password;

    // MÉTHODES UTILITAIRES
    /**
     * Normalise l'email (trim + lowercase)
     */
    public String getNormalizedEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    /**
     * Normalise le nom (trim + capitalize)
     */
    public String getNormalizedName() {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    /**
     * Normalise le numéro de mobile (supprime les espaces)
     */
    public String getNormalizedMobileNumber() {
        return mobileNumber != null ? mobileNumber.replaceAll("\\s+", "") : null;
    }

    /**
     * Vérifie si le mot de passe respecte les critères de sécurité
     */
    public boolean isPasswordSecure() {
        if (password == null) return false;

        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }

    /**
     * Calcule la force du mot de passe
     */
    public PasswordStrength getPasswordStrength() {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.WEAK;
        }

        int score = 0;

        // Longueur minimale
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;

        // Complexité
        if (password.matches(".*[a-z].*")) score++; // minuscule
        if (password.matches(".*[A-Z].*")) score++; // majuscule
        if (password.matches(".*\\d.*")) score++;    // chiffre
        if (password.matches(".*[@$!%*?&].*")) score++; // caractère spécial

        return switch (score) {
            case 0, 1, 2 -> PasswordStrength.WEAK;
            case 3, 4 -> PasswordStrength.MEDIUM;
            case 5, 6 -> PasswordStrength.STRONG;
            default -> PasswordStrength.VERY_STRONG;
        };
    }

    /**
     * Vérifie si les données sont valides pour l'inscription
     */
    public boolean isValidForRegistration() {
        return name != null && !name.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                mobileNumber != null && !mobileNumber.trim().isEmpty() &&
                password != null && !password.trim().isEmpty() &&
                isPasswordSecure();
    }

    /**
     * Enum pour la force du mot de passe
     */
    public enum PasswordStrength {
        WEAK, MEDIUM, STRONG, VERY_STRONG
    }
}