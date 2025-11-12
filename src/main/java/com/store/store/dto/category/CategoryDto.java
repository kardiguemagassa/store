package com.store.store.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CategoryDto {

    private Long categoryId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size.max}")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "{validation.pattern}")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size.max}")
    private String name;

    @Size(max = 500, message = "{validation.size.max}")
    private String description;

    @Size(max = 255, message = "{validation.size.max}")
    private String icon;  // Peut être un emoji ou une URL

    @NotNull(message = "{validation.required}")
    @Min(value = 0, message = "{validation.min.value}")
    private Integer displayOrder;

    @NotNull(message = "{validation.required}")
    private Boolean isActive;

    // Champ calculé (non modifiable) - pas de validation
    private Long productCount;
}