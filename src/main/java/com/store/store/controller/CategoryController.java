package com.store.store.controller;

import com.store.store.dto.CategoryDto;
import com.store.store.dto.ResponseDto;
import com.store.store.dto.SuccessResponseDto;
import com.store.store.service.ICategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Categories", description = "API de gestion des catégories")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CategoryController {

    private final ICategoryService categoryService;

    // =====================================================
    // LECTURE (READ) - PUBLIC
    // =====================================================

    @Operation(summary = "Obtenir toutes les catégories actives",
            description = "Retourne uniquement les catégories actives, triées par ordre d'affichage")
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllActiveCategories() {
        log.info("GET /api/v1/categories - Fetching all active categories");
        List<CategoryDto> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "Obtenir une catégorie par son code",
            description = "Recherche une catégorie par son code unique (ex: SPORTS, ANIME)")
    @GetMapping("/{code}")
    public ResponseEntity<CategoryDto> getCategoryByCode(
            @Parameter(description = "Code de la catégorie (SPORTS, ANIME, etc.)", required = true)
            @PathVariable @NotBlank String code) {

        log.info("GET /api/v1/categories/{} - Fetching category by code", code);
        CategoryDto category = categoryService.getCategoryByCode(code.toUpperCase());
        return ResponseEntity.ok(category);
    }

    @Operation(summary = "Obtenir les catégories avec des produits",
            description = "Retourne uniquement les catégories qui contiennent au moins un produit")
    @GetMapping("/with-products")
    public ResponseEntity<List<CategoryDto>> getCategoriesWithProducts() {
        log.info("GET /api/v1/categories/with-products - Fetching categories with products");
        List<CategoryDto> categories = categoryService.getCategoriesWithProducts();
        return ResponseEntity.ok(categories);
    }

    // =====================================================
    // ADMIN - LECTURE
    // =====================================================

    @Operation(summary = "[ADMIN] Obtenir toutes les catégories",
            description = "Retourne toutes les catégories (actives et inactives)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        log.info("GET /api/v1/categories/admin/all - Fetching all categories");
        List<CategoryDto> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @Operation(summary = "[ADMIN] Obtenir une catégorie par ID")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(
            @Parameter(description = "ID de la catégorie", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/categories/admin/{} - Fetching category by ID", id);
        CategoryDto category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    // =====================================================
    // ADMIN - CRÉATION (CREATE)
    // =====================================================

    @Operation(summary = "[ADMIN] Créer une nouvelle catégorie",
            description = "Crée une catégorie avec code unique, nom, description et icône")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody CategoryDto dto) {

        log.info("POST /api/v1/categories/admin - Creating category: {}", dto.getName());
        CategoryDto created = categoryService.createCategory(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // =====================================================
    // ADMIN - MISE À JOUR (UPDATE)
    // =====================================================

    @Operation(summary = "[ADMIN] Mettre à jour une catégorie",
            description = "Modifie tous les champs d'une catégorie existante")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @Parameter(description = "ID de la catégorie à modifier", required = true)
            @PathVariable @Min(1) Long id,

            @Valid @RequestBody CategoryDto dto) {

        log.info("PUT /api/v1/categories/admin/{} - Updating category", id);
        CategoryDto updated = categoryService.updateCategory(id, dto);

        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "[ADMIN] Activer/Désactiver une catégorie",
            description = "Bascule le statut actif/inactif d'une catégorie")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{id}/toggle-status")
    public ResponseEntity<CategoryDto> toggleCategoryStatus(
            @Parameter(description = "ID de la catégorie", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("PATCH /api/v1/categories/admin/{}/toggle-status", id);
        CategoryDto updated = categoryService.toggleCategoryStatus(id);

        return ResponseEntity.ok(updated);
    }

    // =====================================================
    // ADMIN - SUPPRESSION (DELETE)
    // =====================================================

    @Operation(summary = "[ADMIN] Supprimer une catégorie",
            description = "Supprime une catégorie si elle ne contient pas de produits")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ResponseDto> deleteCategory(
            @Parameter(description = "ID de la catégorie à supprimer", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("DELETE /api/v1/categories/admin/{} - Deleting category", id);
        categoryService.deleteCategory(id);

        return ResponseEntity.ok(
                new ResponseDto("200", "Catégorie supprimée avec succès")
        );
    }

    // =====================================================
    // ADMIN - UPLOAD D'ICÔNE
    // =====================================================

    @Operation(summary = "[ADMIN] Uploader une icône pour une catégorie",
            description = "Upload d'une icône personnalisée (PNG, SVG, JPEG, WebP). Taille max: 2 MB")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDto> uploadCategoryIcon(
            @Parameter(description = "ID de la catégorie", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichier icône (PNG, SVG, JPEG, WebP)", required = true)
            @RequestParam("icon") MultipartFile iconFile) {

        log.info("POST /api/v1/categories/admin/{}/icon - Uploading icon: {}",
                id, iconFile.getOriginalFilename());

        String iconUrl = categoryService.uploadCategoryIcon(id, iconFile);

        return ResponseEntity.ok(
                SuccessResponseDto.of(
                        "Icône uploadée avec succès: " + iconUrl,
                        HttpStatus.OK.value()
                )
        );
    }

    @Operation(summary = "[ADMIN] Définir une icône emoji",
            description = "Définit un emoji comme icône de catégorie")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{id}/emoji")
    public ResponseEntity<CategoryDto> setCategoryEmoji(
            @Parameter(description = "ID de la catégorie", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Emoji à utiliser", required = true)
            @RequestParam @NotBlank String emoji) {

        log.info("PATCH /api/v1/categories/admin/{}/emoji - Setting emoji: {}", id, emoji);

        // Récupérer la catégorie
        CategoryDto category = categoryService.getCategoryById(id);

        // Mettre à jour l'icône avec l'emoji
        category.setIcon(emoji);
        CategoryDto updated = categoryService.updateCategory(id, category);

        return ResponseEntity.ok(updated);
    }
}