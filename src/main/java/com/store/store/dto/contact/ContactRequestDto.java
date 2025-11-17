package com.store.store.dto.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * @author Kardigué
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Données du formulaire de contact")
public class ContactRequestDto {

    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 50, message = "{validation.size.range}")
    @Schema(description = "Nom complet du contact", example = "Jean Dupont", required = true)
    private String name;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 100, message = "{validation.size.max}")
    @Schema(description = "Adresse email", example = "jean.dupont@example.com", required = true)
    private String email;

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "^\\d{10}$", message = "{validation.mobileNumber.pattern}")
    @Schema(description = "Numéro de mobile (10 chiffres)", example = "0612345678", required = true)
    private String mobileNumber;

    @NotBlank(message = "{validation.required}")
    @Size(min = 10, max = 1000, message = "{validation.size.range}")
    @Schema(description = "Message du contact", example = "Je souhaite des informations sur vos produits", required = true)
    private String message;
}