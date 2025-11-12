package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.product.ProductDto;
import com.store.store.dto.product.ProductSearchCriteria;
import com.store.store.service.IProductService;

import com.store.store.service.impl.MessageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-01
 */
@Tag(name = "Products", description = "API de gestion des produits")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ProductController {

    private final IProductService productService;
    private final MessageServiceImpl messageService;

    // ENDPOINTS PUBLICS - RECHERCHE ET CONSULTATION
    @Operation(
            summary = "Rechercher des produits avec filtres avancés",
            description = "Recherche multi-critères avec pagination, tri et filtres (catégorie, prix, stock)"
    )
    @GetMapping("/search")
    public ResponseEntity<Page<ProductDto>> searchProducts(
            @Parameter(description = "Terme de recherche (nom ou description)")
            @RequestParam(required = false) String query,

            @Parameter(description = "Code de catégorie (ex: ELECTRONICS, CLOTHING)")
            @RequestParam(required = false) String category,

            @Parameter(description = "Prix minimum")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Prix maximum")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Uniquement les produits en stock")
            @RequestParam(defaultValue = "false") boolean inStockOnly,

            @Parameter(description = "Uniquement les produits actifs")
            @RequestParam(defaultValue = "true") boolean activeOnly,

            @Parameter(description = "Champ de tri (NAME, PRICE, POPULARITY, CREATED_DATE)")
            @RequestParam(defaultValue = "NAME") ProductSearchCriteria.SortBy sortBy,

            @Parameter(description = "Direction du tri (ASC, DESC)")
            @RequestParam(defaultValue = "ASC") ProductSearchCriteria.SortDirection sortDirection,

            @Parameter(description = "Numéro de page (0-based)")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Taille de la page")
            @RequestParam(defaultValue = "12") @Min(1) int size) {

        log.info("GET /api/v1/products/search - query: {}, category: {}, page: {}, size: {}",
                query, category, page, size);

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .searchQuery(query)
                .categoryCode(category)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStockOnly(inStockOnly)
                .activeOnly(activeOnly)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDto> products = productService.searchProducts(criteria, pageable);

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Obtenir tous les produits actifs")
    @GetMapping
    public ResponseEntity<List<ProductDto>> getActiveProducts() {
        log.info("GET /api/v1/products - Fetching all active products");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(true)
                .build();

        Page<ProductDto> products = productService.searchProducts(criteria, PageRequest.of(0, 1000));
        return ResponseEntity.ok(products.getContent());
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

    @Operation(summary = "Obtenir les produits actifs avec pagination")
    @GetMapping("/paginated")
    public ResponseEntity<Page<ProductDto>> getActiveProductsPaginated(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) int size,
            @RequestParam(defaultValue = "NAME") ProductSearchCriteria.SortBy sortBy,
            @RequestParam(defaultValue = "ASC") ProductSearchCriteria.SortDirection sortDirection) {

        log.info("GET /api/v1/products/paginated - page: {}, size: {}", page, size);

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(true)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDto> products = productService.searchProducts(criteria, pageable);

        return ResponseEntity.ok(products);
    }

    @Operation(
            summary = "Récupérer les produits inactifs",
            description = "Retourne la liste paginée des produits marqués comme inactifs (soft deleted)",
            tags = {"Products - Admin"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des produits inactifs récupérée avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit - Rôle admin requis"
            )
    })
    @GetMapping("/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductDto>> getInactiveProducts(
            @PageableDefault(size = 10, sort = "name") Pageable pageable) {

        log.info("GET /api/v1/products/inactive - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<ProductDto> inactiveProducts = productService.getInactiveProducts(pageable);

        log.info("Returning {} inactive products (total: {})",
                inactiveProducts.getNumberOfElements(),
                inactiveProducts.getTotalElements());

        return ResponseEntity.ok(inactiveProducts);
    }

    @Operation(
            summary = "Obtenir les produits inactifs avec pagination",
            description = "Retourne la liste paginée des produits inactifs (soft deleted) " +
                    "avec options de tri personnalisées. Accessible uniquement aux administrateurs.",
            tags = {"Products - Admin"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des produits inactifs récupérée avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Paramètres de pagination invalides"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit - Rôle admin requis"
            )
    })
    @GetMapping("/inactive/paginated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ProductDto>> getInactiveProductsPaginated(
            @Parameter(description = "Numéro de page (commence à 0)")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "Nombre d'éléments par page")
            @RequestParam(defaultValue = "12") @Min(1) int size,

            @Parameter(description = "Critère de tri")
            @RequestParam(defaultValue = "CREATED_DATE") ProductSearchCriteria.SortBy sortBy,

            @Parameter(description = "Direction du tri")
            @RequestParam(defaultValue = "DESC") ProductSearchCriteria.SortDirection sortDirection) {

        log.info("GET /api/v1/products/inactive/paginated - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                page, size, sortBy, sortDirection);

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(false)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDto> products = productService.searchProducts(criteria, pageable);

        log.info("Returning {} inactive products (total: {}, page: {}/{})",
                products.getNumberOfElements(),
                products.getTotalElements(),
                page + 1,
                products.getTotalPages());

        return ResponseEntity.ok(products);
    }

    @Operation(
            summary = "Compter les produits inactifs",
            description = "Retourne le nombre total de produits marqués comme inactifs",
            tags = {"Products - Statistics"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Nombre de produits inactifs",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Long.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès interdit - Rôle admin requis"
            )
    })
    @GetMapping("/count/inactive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> countInactiveProducts() {
        log.info("GET /api/v1/products/count/inactive");

        long count = productService.countInactiveProducts();

        log.info("Total inactive products: {}", count);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Obtenir les produits par catégorie")
    @GetMapping("/category/{categoryCode}")
    public ResponseEntity<Page<ProductDto>> getProductsByCategory(
            @Parameter(description = "Code de la catégorie", required = true)
            @PathVariable @NotBlank String categoryCode,

            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) int size) {

        log.info("GET /api/v1/products/category/{} - page: {}, size: {}", categoryCode, page, size);

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .categoryCode(categoryCode.toUpperCase())
                .activeOnly(true)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDto> products = productService.searchProducts(criteria, pageable);

        return ResponseEntity.ok(products);
    }

    // ENDPOINTS SPÉCIALISÉS
    @Operation(summary = "Obtenir les produits populaires")
    @GetMapping("/featured")
    public ResponseEntity<Page<ProductDto>> getFeaturedProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "8") @Min(1) int size) {

        log.info("GET /api/v1/products/featured - page: {}, size: {}", page, size);

        Page<ProductDto> products = productService.getFeaturedProducts(PageRequest.of(page, size));
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Obtenir les produits en promotion")
    @GetMapping("/on-sale")
    public ResponseEntity<Page<ProductDto>> getProductsOnSale(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "8") @Min(1) int size) {

        log.info("GET /api/v1/products/on-sale - page: {}, size: {}", page, size);

        Page<ProductDto> products = productService.getProductsOnSale(PageRequest.of(page, size));
        return ResponseEntity.ok(products);
    }

    // ENDPOINTS ADMIN - CONSULTATION
    @Operation(summary = "[ADMIN] Obtenir tous les produits (y compris inactifs)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<List<ProductDto>> getAllProductsAdmin() {
        log.info("GET /api/v1/products/admin/all - Fetching all products");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(false)
                .build();

        Page<ProductDto> products = productService.searchProducts(criteria, PageRequest.of(0, 1000));
        return ResponseEntity.ok(products.getContent());
    }


    @Operation(summary = "[ADMIN] Obtenir tous les produits avec pagination")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/paginated")
    public ResponseEntity<Page<ProductDto>> getAllProductsPaginatedAdmin(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) int size,
            @RequestParam(defaultValue = "NAME") ProductSearchCriteria.SortBy sortBy,
            @RequestParam(defaultValue = "ASC") ProductSearchCriteria.SortDirection sortDirection) {

        log.info("GET /api/v1/products/admin/paginated - page: {}, size: {}", page, size);

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(false)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDto> products = productService.searchProducts(criteria, pageable);

        return ResponseEntity.ok(products);
    }

    @Operation(summary = "[ADMIN] Obtenir les statistiques des produits")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductStats() {
        log.info("GET /api/v1/products/admin/stats - Fetching product statistics");

        long activeCount = productService.countActiveProducts();
        long outOfStockCount = productService.countOutOfStockProducts();

        Map<String, Object> stats = Map.of(
                "activeProducts", activeCount,
                "outOfStockProducts", outOfStockCount,
                "totalProducts", activeCount + outOfStockCount
        );

        String message = messageService.getMessage("api.success.stats.retrieved");

        ApiResponse<Map<String, Object>> response = ApiResponse.success(message, stats)
                .withPath("/api/v1/products/admin/stats");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[ADMIN] Créer un nouveau produit")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDto> createProduct(
            @Valid @RequestBody ProductDto productDto) {

        log.info("POST /api/v1/products - Creating product: {}", productDto.getName());

        ProductDto createdProduct = productService.createProduct(productDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProduct);
    }


    @Operation(summary = "[ADMIN] Mettre à jour un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @Parameter(description = "ID du produit à modifier", required = true)
            @PathVariable @Min(1) Long id,

            @Valid @RequestBody ProductDto productDto) {

        log.info("PUT /api/v1/products/{} - Updating product", id);

        ProductDto updatedProduct = productService.updateProduct(id, productDto);

        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "[ADMIN] Mettre à jour partiellement un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> patchProduct(
            @PathVariable @Min(1) Long id,
            @RequestBody ProductDto productDto) {

        log.info("PATCH /api/v1/products/{} - Partial update", id);

        ProductDto updatedProduct = productService.updateProduct(id, productDto);

        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "[ADMIN] Supprimer un produit (soft delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "ID du produit à supprimer", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("DELETE /api/v1/products/{} - Soft deleting product", id);

        productService.deleteProduct(id);

        String message = messageService.getMessage("api.success.product.deleted");

        ApiResponse<Void> response = ApiResponse.<Void>success(message)
                .withPath("/api/v1/products/" + id);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[ADMIN] Restaurer un produit supprimé")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{id}/restore")
    public ResponseEntity<ProductDto> restoreProduct(
            @Parameter(description = "ID du produit à restaurer", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("PATCH /api/v1/products/admin/{}/restore - Restoring product", id);

        ProductDto restoredProduct = productService.restoreProduct(id);

        return ResponseEntity.ok(restoredProduct);
    }

    // ENDPOINTS ADMIN - GESTION DES IMAGES
    @Operation(summary = "[ADMIN] Uploader une image pour un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichier image", required = true)
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        log.info("POST /api/v1/products/{}/image - Uploading image: {}",
                id, imageFile.getOriginalFilename());

        String imageUrl = productService.uploadProductImage(id, imageFile);

        String message = messageService.getMessage("api.success.image.uploaded");

        ApiResponse<Map<String, String>> response = ApiResponse.success(
                message,
                Map.of("imageUrl", imageUrl)
        ).withPath("/api/v1/products/" + id + "/image");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[ADMIN] Supprimer l'image d'un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/image")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("DELETE /api/v1/products/{}/image - Deleting product image", id);

        productService.deleteProductImage(id);

        String message = messageService.getMessage("api.success.image.deleted");

        ApiResponse<Void> response = ApiResponse.<Void>success(message)
                .withPath("/api/v1/products/" + id + "/image");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Récupérer les bytes d'une image produit")
    @GetMapping("/{id}/image/bytes")
    public ResponseEntity<byte[]> getProductImageBytes(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/products/{}/image/bytes - Fetching image bytes", id);

        byte[] imageBytes = productService.getProductImageBytes(id);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    // ENDPOINTS ADMIN - GALERIE D'IMAGES
    @Operation(summary = "[ADMIN] Uploader plusieurs images pour un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadProductImages(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichiers images", required = true)
            @RequestParam("images") List<MultipartFile> imageFiles) throws IOException {

        log.info("POST /api/v1/products/{}/gallery - Uploading {} images", id, imageFiles.size());

        List<String> imageUrls = productService.uploadProductImages(id, imageFiles);

        String message = messageService.getMessage(
                "api.success.images.uploaded.count",
                imageUrls.size()
        );

        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                message,
                Map.of("imageUrls", imageUrls, "count", imageUrls.size())
        ).withPath("/api/v1/products/" + id + "/gallery");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[ADMIN] Ajouter une image à la galerie")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/gallery/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> addToProductGallery(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichier image", required = true)
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        log.info("POST /api/v1/products/{}/gallery/single - Adding image to gallery", id);

        String imageUrl = productService.addToProductGallery(id, imageFile);

        String message = messageService.getMessage("api.success.image.added.gallery");

        ApiResponse<Map<String, String>> response = ApiResponse.success(
                message,
                Map.of("imageUrl", imageUrl)
        ).withPath("/api/v1/products/" + id + "/gallery/single");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[ADMIN] Supprimer une image de la galerie")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{productId}/gallery")
    public ResponseEntity<ApiResponse<Void>> removeFromProductGallery(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long productId,

            @Parameter(description = "URL de l'image à supprimer", required = true)
            @RequestParam String imageUrl) {

        log.info("DELETE /api/v1/products/{}/gallery - Removing image: {}", productId, imageUrl);

        productService.removeFromProductGallery(productId, imageUrl);

        String message = messageService.getMessage("api.success.image.removed.gallery");

        ApiResponse<Void> response = ApiResponse.<Void>success(message)
                .withPath("/api/v1/products/" + productId + "/gallery");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "[ADMIN] Réorganiser la galerie d'images")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/gallery/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderGalleryImages(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @RequestBody List<String> imageUrlsInOrder) {

        log.info("PUT /api/v1/products/{}/gallery/reorder - Reordering {} images",
                id, imageUrlsInOrder.size());

        productService.reorderGalleryImages(id, imageUrlsInOrder);

        String message = messageService.getMessage("api.success.gallery.reordered");

        ApiResponse<Void> response = ApiResponse.<Void>success(message)
                .withPath("/api/v1/products/" + id + "/gallery/reorder");

        return ResponseEntity.ok(response);
    }
}