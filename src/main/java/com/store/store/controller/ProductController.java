package com.store.store.controller;

import com.store.store.dto.ProductDto;
import com.store.store.dto.ResponseDto;
import com.store.store.dto.SuccessResponseDto;
import com.store.store.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Products", description = "API de gestion des produits")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ProductController {

    private final IProductService productService;

    // =====================================================
    // LECTURE (READ)
    // =====================================================

    @Operation(summary = "Obtenir tous les produits",
            description = "Retourne la liste complète des produits sans pagination")
    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts() {
        log.info("GET /api/v1/products - Fetching all products");
        List<ProductDto> productList = productService.getProducts();
        return ResponseEntity.ok(productList);
    }

    @Operation(summary = "Obtenir un produit par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/products/{} - Fetching product", id);
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    // =====================================================
    // PRODUITS PAR CATÉGORIE (CORRIGÉ)
    // =====================================================

    @Operation(summary = "Obtenir les produits par code de catégorie",
            description = "Retourne tous les produits d'une catégorie (ex: SPORTS, ANIME)")
    @GetMapping("/category/{categoryCode}")
    public ResponseEntity<List<ProductDto>> getProductsByCategory(
            @Parameter(description = "Code de la catégorie (SPORTS, ANIME, etc.)", required = true)
            @PathVariable @NotBlank String categoryCode) {

        log.info("GET /api/v1/products/category/{} - Fetching products by category", categoryCode);

        // ✅ CORRIGÉ : Utiliser categoryCode au lieu de category
        List<ProductDto> products = productService.getProductsByCategoryCode(
                categoryCode.toUpperCase()
        );

        return ResponseEntity.ok(products);
    }

    // =====================================================
    // PAGINATION
    // =====================================================

    @Operation(summary = "Obtenir les produits avec pagination",
            description = "Supporte le tri et la pagination")
    @GetMapping("/paginated")
    public ResponseEntity<Page<ProductDto>> getProductsPaginated(
            @Parameter(description = "Numéro de page (commence à 0)")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Nombre d'éléments par page")
            @RequestParam(defaultValue = "12") @Min(1) int size,

            @Parameter(description = "Champ de tri (name, price, popularity)")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "Direction du tri (asc ou desc)")
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("GET /api/v1/products/paginated - page: {}, size: {}, sortBy: {}",
                page, size, sortBy);

        // Validation du champ de tri
        if (!isValidSortField(sortBy)) {
            sortBy = "name"; // Valeur par défaut sécurisée
        }

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductDto> products = productService.getProducts(pageable);

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Obtenir les produits par catégorie avec pagination")
    @GetMapping("/category/{categoryCode}/paginated")
    public ResponseEntity<Page<ProductDto>> getProductsByCategoryPaginated(
            @Parameter(description = "Code de la catégorie", required = true)
            @PathVariable @NotBlank String categoryCode,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("GET /api/v1/products/category/{}/paginated - page: {}, size: {}",
                categoryCode, page, size);

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // ✅ CORRIGÉ
        Page<ProductDto> products = productService.getProductsByCategoryCode(
                categoryCode.toUpperCase(),
                pageable
        );

        return ResponseEntity.ok(products);
    }

    // =====================================================
    // RECHERCHE
    // =====================================================

    @Operation(summary = "Rechercher des produits",
            description = "Recherche par nom ou description")
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @Parameter(description = "Terme de recherche", required = true)
            @RequestParam @NotBlank String query,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) int size) {

        log.info("GET /api/v1/products/search?query={} - page: {}, size: {}",
                query, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDto> products = productService.searchProducts(query, pageable);

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Rechercher des produits dans une catégorie")
    @GetMapping("/category/{categoryCode}/search")
    public ResponseEntity<Page<ProductDto>> searchProductsByCategory(
            @Parameter(description = "Code de la catégorie", required = true)
            @PathVariable @NotBlank String categoryCode,

            @Parameter(description = "Terme de recherche", required = true)
            @RequestParam @NotBlank String query,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) int size) {

        log.info("GET /api/v1/products/category/{}/search?query={}", categoryCode, query);

        Pageable pageable = PageRequest.of(page, size);

        // ✅ CORRIGÉ
        Page<ProductDto> products = productService.searchProductsByCategoryCode(
                categoryCode.toUpperCase(),
                query,
                pageable
        );

        return ResponseEntity.ok(products);
    }

    // =====================================================
    // CRÉATION (CREATE)
    // =====================================================

    @Operation(summary = "Créer un nouveau produit",
            description = "Nécessite les droits ADMIN")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody ProductDto productDto) {

        log.info("POST /api/v1/products - Creating product: {}", productDto.getName());

        ProductDto createdProduct = productService.createProduct(productDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProduct);
    }

    // =====================================================
    // MISE À JOUR (UPDATE)
    // =====================================================

    @Operation(summary = "Mettre à jour un produit",
            description = "Nécessite les droits ADMIN")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @Parameter(description = "ID du produit à modifier", required = true)
            @PathVariable @Min(1) Long id,

            @Valid @RequestBody ProductDto productDto) {

        log.info("PUT /api/v1/products/{} - Updating product", id);

        ProductDto updatedProduct = productService.updateProduct(id, productDto);

        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Mettre à jour partiellement un produit (PATCH)",
            description = "Permet de modifier uniquement certains champs")
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> patchProduct(
            @PathVariable @Min(1) Long id,
            @RequestBody ProductDto productDto) {

        log.info("PATCH /api/v1/products/{} - Partial update", id);

        // Note: Implémenter une logique de mise à jour partielle dans le service
        ProductDto updatedProduct = productService.updateProduct(id, productDto);

        return ResponseEntity.ok(updatedProduct);
    }

    // =====================================================
    // SUPPRESSION (DELETE)
    // =====================================================

    @Operation(summary = "Supprimer un produit",
            description = "Nécessite les droits ADMIN. Suppression définitive.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto> deleteProduct(
            @Parameter(description = "ID du produit à supprimer", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("DELETE /api/v1/products/{} - Deleting product", id);

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                new ResponseDto("200", "Produit supprimé avec succès")
        );
    }

    // =====================================================
    // UPLOAD D'IMAGE
    // =====================================================

    @Operation(summary = "Uploader une image pour un produit",
            description = "Formats acceptés: JPEG, PNG, WebP, GIF. Taille max: 5 MB")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDto> uploadProductImage(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichier image", required = true)
            @RequestParam("image") MultipartFile imageFile) {

        log.info("POST /api/v1/products/{}/image - Uploading image: {}",
                id, imageFile.getOriginalFilename());

        String imageUrl = productService.uploadProductImage(id, imageFile);

        return ResponseEntity.ok(
                SuccessResponseDto.of(
                        "Image uploadée avec succès: " + imageUrl,
                        HttpStatus.OK.value()
                )
        );
    }

    // =====================================================
    // MÉTHODES UTILITAIRES
    // =====================================================

    /**
     * Valide que le champ de tri est autorisé
     */
    private boolean isValidSortField(String sortBy) {
        return List.of("name", "price", "popularity", "createdAt")
                .contains(sortBy);
    }
}