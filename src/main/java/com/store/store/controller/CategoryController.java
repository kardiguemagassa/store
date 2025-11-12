package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.category.CategoryDto;
import com.store.store.service.ICategoryService;

import com.store.store.service.impl.MessageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import java.util.Map;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-06
 */
@Tag(name = "Categories", description = "API de gestion des catégories de produits")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class CategoryController {

    private final ICategoryService categoryService;
    private final MessageServiceImpl messageService;

    // ENDPOINTS PUBLICS - LECTURE
    @Operation(
            summary = "Obtenir toutes les catégories actives",
            description = "Retourne uniquement les catégories actives, triées par ordre d'affichage"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des catégories actives récupérée avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllActiveCategories() {
        log.info("GET /api/v1/categories - Fetching all active categories");

        // Appel au service
        List<CategoryDto> categories = categoryService.getAllActiveCategories();

        log.info("Found {} active categories", categories.size());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.categories.active.retrieved",
                categories.size()
        );

        ApiResponse<List<CategoryDto>> response = ApiResponse.success(successMessage, categories)
                .withPath("/api/v1/categories");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtenir une catégorie par son code",
            description = "Recherche une catégorie par son code unique (ex: SPORTS, ANIME). Insensible à la casse."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Catégorie trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Catégorie non trouvée avec ce code"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Code invalide (vide ou trop long)"
            )
    })
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryByCode(
            @Parameter(description = "Code de la catégorie (SPORTS, ANIME, etc.)", required = true)
            @PathVariable @NotBlank String code) {

        log.info("GET /api/v1/categories/{} - Fetching category by code", code);

        // Appel au service (conversion en majuscules)
        CategoryDto category = categoryService.getCategoryByCode(code.toUpperCase());

        log.info("Category found: {}", category.getName());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.category.retrieved.byCode",
                code.toUpperCase()
        );

        ApiResponse<CategoryDto> response = ApiResponse.success(successMessage, category)
                .withPath("/api/v1/categories/" + code);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Obtenir les catégories avec des produits",
            description = "Retourne uniquement les catégories actives qui contiennent au moins un produit"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des catégories avec produits"
            )
    })
    @GetMapping("/with-products")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategoriesWithProducts() {
        log.info("GET /api/v1/categories/with-products - Fetching categories with products");

        // Appel au service
        List<CategoryDto> categories = categoryService.getCategoriesWithProducts();

        log.info("Found {} categories with products", categories.size());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.categories.withProducts.retrieved",
                categories.size()
        );

        ApiResponse<List<CategoryDto>> response = ApiResponse.success(successMessage, categories)
                .withPath("/api/v1/categories/with-products");

        return ResponseEntity.ok(response);
    }

    // ENDPOINTS ADMIN - LECTURE
    @Operation(
            summary = "[ADMIN] Obtenir toutes les catégories",
            description = "Retourne toutes les catégories (actives et inactives). Accès ADMIN uniquement."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste complète récupérée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (rôle ADMIN requis)"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategories() {
        log.info("GET /api/v1/categories/admin/all - Fetching all categories (including inactive)");

        // Appel au service
        List<CategoryDto> categories = categoryService.getAllCategories();

        log.info("Found {} categories total", categories.size());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.categories.all.retrieved",
                categories.size()
        );

        ApiResponse<List<CategoryDto>> response = ApiResponse.success(successMessage, categories)
                .withPath("/api/v1/categories/admin/all");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "[ADMIN] Obtenir une catégorie par ID",
            description = "Récupère une catégorie spécifique par son identifiant numérique"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Catégorie trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Catégorie non trouvée"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> getCategoryById(
            @Parameter(description = "ID de la catégorie", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/categories/admin/{} - Fetching category by ID", id);

        // Appel au service
        CategoryDto category = categoryService.getCategoryById(id);

        log.info("Category found: {}", category.getName());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.category.retrieved.byId",
                id
        );

        ApiResponse<CategoryDto> response = ApiResponse.success(successMessage, category)
                .withPath("/api/v1/categories/admin/" + id);

        return ResponseEntity.ok(response);
    }

    // ENDPOINTS ADMIN - CRÉATION
    @Operation(
            summary = "[ADMIN] Créer une nouvelle catégorie",
            description = "Crée une catégorie avec code unique, nom, description et icône"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Catégorie créée avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données invalides ou code déjà existant"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit (rôle ADMIN requis)"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(
            @Valid @RequestBody CategoryDto dto) {

        log.info("POST /api/v1/categories/admin - Creating category: {}", dto.getName());

        // Appel au service
        CategoryDto created = categoryService.createCategory(dto);

        log.info("Category created successfully with ID: {}", created.getCategoryId());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.category.created",
                created.getName()
        );

        ApiResponse<CategoryDto> response = ApiResponse.created(successMessage, created)
                .withPath("/api/v1/categories/admin");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ENDPOINTS ADMIN - MISE À JOUR
    @Operation(
            summary = "[ADMIN] Mettre à jour une catégorie",
            description = "Modifie tous les champs d'une catégorie existante"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Catégorie mise à jour avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Catégorie non trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données invalides ou code déjà utilisé"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @Parameter(description = "ID de la catégorie à modifier", required = true)
            @PathVariable @Min(1) Long id,

            @Valid @RequestBody CategoryDto dto) {

        log.info("PUT /api/v1/categories/admin/{} - Updating category", id);

        // Appel au service
        CategoryDto updated = categoryService.updateCategory(id, dto);

        log.info("Category updated successfully: {}", updated.getName());

        // Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.category.updated",
                updated.getName()
        );

        ApiResponse<CategoryDto> response = ApiResponse.success(successMessage, updated)
                .withPath("/api/v1/categories/admin/" + id);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "[ADMIN] Activer/Désactiver une catégorie",
            description = "Bascule le statut actif/inactif d'une catégorie (toggle)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Statut basculé avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Catégorie non trouvée"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{id}/toggle-status")
    public ResponseEntity<ApiResponse<CategoryDto>> toggleCategoryStatus(
            @Parameter(description = "ID de la catégorie", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("PATCH /api/v1/categories/admin/{}/toggle-status", id);

        // Appel au service
        CategoryDto updated = categoryService.toggleCategoryStatus(id);

        log.info("Category status toggled: {} is now {}",
                updated.getName(), updated.getIsActive() ? "active" : "inactive");

        // Message de succès localisé avec état actuel
        String statusKey = updated.getIsActive() ?
                "api.success.category.status.toggled.active" :
                "api.success.category.status.toggled.inactive";

        String successMessage = messageService.getMessage(statusKey, updated.getName());

        ApiResponse<CategoryDto> response = ApiResponse.success(successMessage, updated)
                .withPath("/api/v1/categories/admin/" + id + "/toggle-status");

        return ResponseEntity.ok(response);
    }


    // ENDPOINTS ADMIN - SUPPRESSION
    @Operation(
            summary = "[ADMIN] Supprimer une catégorie",
            description = "Supprime définitivement une catégorie si elle ne contient pas de produits"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Catégorie supprimée avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Catégorie non trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Impossible de supprimer : la catégorie contient des produits"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "ID de la catégorie à supprimer", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("DELETE /api/v1/categories/admin/{} - Deleting category", id);

        // Appel au service (peut lever BusinessException si produits présents)
        categoryService.deleteCategory(id);

        log.info("Category {} deleted successfully", id);

        // Message de succès localisé
        String successMessage = messageService.getMessage("api.success.category.deleted");

        ApiResponse<Void> response = ApiResponse.<Void>success(successMessage)
                .withPath("/api/v1/categories/admin/" + id);

        return ResponseEntity.ok(response);
    }

    // ENDPOINTS ADMIN - GESTION DES ICÔNES
    @Operation(
            summary = "[ADMIN] Uploader une icône pour une catégorie",
            description = "Upload d'une icône personnalisée (PNG, SVG, JPEG, WebP). Taille max: 2 MB"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Icône uploadée avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Catégorie non trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Fichier invalide (type ou taille)"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/admin/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCategoryIcon(
            @Parameter(description = "ID de la catégorie", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichier icône (PNG, SVG, JPEG, WebP)", required = true)
            @RequestParam("icon") MultipartFile iconFile) {

        log.info("POST /api/v1/categories/admin/{}/icon - Uploading icon: {}",
                id, iconFile.getOriginalFilename());

        // Appel au service
        String iconUrl = categoryService.uploadCategoryIcon(id, iconFile);

        log.info("Icon uploaded successfully: {}", iconUrl);

        // Message de succès localisé
        String successMessage = messageService.getMessage("api.success.category.icon.uploaded");

        ApiResponse<Map<String, String>> response = ApiResponse.success(
                successMessage,
                Map.of("iconUrl", iconUrl)
        ).withPath("/api/v1/categories/admin/" + id + "/icon");

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "[ADMIN] Définir une icône emoji",
            description = "Définit un emoji comme icône de catégorie "
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Emoji défini avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Catégorie non trouvée"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Emoji invalide (vide)"
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{id}/emoji")
    public ResponseEntity<ApiResponse<CategoryDto>> setCategoryEmoji(
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

        log.info("Emoji set successfully for category: {}", updated.getName());

        //Message de succès localisé
        String successMessage = messageService.getMessage(
                "api.success.category.emoji.set",
                updated.getName()
        );

        ApiResponse<CategoryDto> response = ApiResponse.success(successMessage, updated)
                .withPath("/api/v1/categories/admin/" + id + "/emoji");

        return ResponseEntity.ok(response);
    }
}