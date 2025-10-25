package com.store.store.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CategoryDto {

    private Long categoryId;

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 50, message = "Le code ne doit pas dépasser 50 caractères")
    @Pattern(regexp = "^[A-Z_]+$", message = "Le code doit être en majuscules avec underscores")
    private String code;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    @Size(max = 255, message = "L'icône ne doit pas dépasser 255 caractères")
    private String icon;  // Peut être un emoji ou une URL

    @Min(value = 0, message = "L'ordre d'affichage doit être positif")
    private Integer displayOrder;

    private Boolean isActive;

    // Champ calculé (non modifiable)
    private Long productCount;
}