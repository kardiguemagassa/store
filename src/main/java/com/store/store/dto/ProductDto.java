package com.store.store.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDto {

    private Long productId;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 250, message = "Le nom ne doit pas dépasser 250 caractères")
    private String name;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0")
    private BigDecimal price;

    @Min(value = 0, message = "La popularité doit être positive")
    private Integer popularity;

    @Size(max = 500, message = "L'URL de l'image ne doit pas dépasser 500 caractères")
    private String imageUrl;

    // ✅ NOUVEAU : Relation avec Category
    @NotNull(message = "La catégorie est obligatoire")
    private Long categoryId;

    // ✅ Champs supplémentaires pour l'affichage (non modifiables)
    private String categoryCode;
    private String categoryName;
    private String categoryIcon;
}