package com.store.store.controller;

import com.store.store.dto.ProductDto;
import com.store.store.dto.ProductSearchCriteria;
import com.store.store.dto.ResponseDto;
import com.store.store.dto.SuccessResponseDto;
import com.store.store.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

@Tag(name = "Products", description = "API de gestion des produits")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ProductController {

    private final IProductService productService;

    // ENDPOINT UNIVERSELLEMENT RECHERCHE

    /**
     * Searches for products with advanced filtering, sorting, and pagination.
     *
     * @param query         the search term matching product name or description (optional)
     * @param category      the category code to filter products (e.g., ELECTRONICS, CLOTHING) (optional)
     * @param minPrice      the minimum price for filtering products (optional)
     * @param maxPrice      the maximum price for filtering products (optional)
     * @param inStockOnly   whether to filter products that are in stock (default is false)
     * @param activeOnly    whether to filter only active products (default is true)
     * @param sortBy        the field to sort by (options: NAME, PRICE, POPULARITY, CREATED_DATE) (default is NAME)
     * @param sortDirection the sort direction (ASC for ascending, DESC for descending) (default is ASC)
     * @param page          the page number for pagination, 0-based (default is 0, minimum is 0)
     * @param size          the size of the page for pagination (default is 12, minimum is 1)
     * @return a paginated response containing a list of matching product DTOs
     */
    @Operation(summary = "Rechercher des produits avec filtres avancés",
            description = "Recherche multi-critères avec pagination, tri et filtres")
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

    // ENDPOINTS PUBLICS SIMPLIFIÉS (compatibilité)

    /**
     * Fetches all active products based on the specified criteria.
     *
     * @return a ResponseEntity containing a list of active product DTOs
     */
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

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param id the unique identifier of the product. Must be a positive number greater than or equal to 1.
     * @return a ResponseEntity containing the product data as a ProductDto if found, or an appropriate error response.
     */
    @Operation(summary = "Obtenir un produit par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("GET /api/v1/products/{} - Fetching product", id);
        ProductDto product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Retrieves active products with pagination. The results can be sorted by a specific field
     * and order using the provided criteria.
     *
     * @param page the page number to retrieve, starting from 0 (default is 0; must be greater than or equal to 0)
     * @param size the number of items per page (default is 12; must be greater than or equal to 1)
     * @param sortBy the field to sort the results by (default is "NAME")
     * @param sortDirection the direction of sorting, either ASC (ascending) or DESC (descending) (default is "ASC")
     * @return a {@link ResponseEntity} containing a {@link Page} of {@link ProductDto} objects representing active products
     */
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

    /**
     * Retrieves a paginated list of products that are marked as inactive (soft deleted).
     * Only accessible to users with the admin role.
     *
     * @param pageable the pagination and sorting information, including page size, page number, and sort criteria
     * @return a ResponseEntity containing a paginated list of inactive products as ProductDto objects
     */
    @Operation(
            summary = "Récupérer les produits inactifs",
            description = "Retourne la liste paginée des produits marqués comme inactifs (soft deleted)",
            tags = {"Products - Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des produits inactifs récupérée avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @ApiResponse(
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

    /**
     * Retrieves a paginated list of inactive (soft deleted) products with custom sorting options.
     * Accessible only to administrators.
     *
     * @param page the page number to retrieve (starting from 0); defaults to 0 if not provided
     * @param size the number of items per page; defaults to 12 if not provided
     * @param sortBy the field by which to sort the results; defaults to CREATED_DATE if not provided
     * @param sortDirection the direction of the sorting (ASC or DESC); defaults to DESC if not provided
     * @return a ResponseEntity containing a paginated list of ProductDto objects representing the inactive products
     */
    @Operation(
            summary = "Obtenir les produits inactifs avec pagination",
            description = "Retourne la liste paginée des produits inactifs (soft deleted) avec options de tri personnalisées. Accessible uniquement aux administrateurs.",
            tags = {"Products - Admin"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Liste des produits inactifs récupérée avec succès",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Paramètres de pagination invalides"
            ),
            @ApiResponse(
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

        log.info("📥 GET /api/v1/products/inactive/paginated - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                page, size, sortBy, sortDirection);

        // Critères de recherche pour produits INACTIFS
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(false)  // ← INACTIFS (isActive = false)
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

    /**
     * Counts the total number of inactive products in the system.
     *
     * This method is accessible only to users with admin privileges.
     *
     * @return a ResponseEntity containing the total number of inactive products.
     *         Returns an HTTP 200 response with a Long value on success.
     *         If access is denied, an HTTP 403 response is returned.
     */
    @Operation(
            summary = "Compter les produits inactifs",
            description = "Retourne le nombre total de produits marqués comme inactifs",
            tags = {"Products - Statistics"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Nombre de produits inactifs",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Long.class)
                    )
            ),
            @ApiResponse(
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

    /**
     * Retrieves a paginated list of products for a specified category.
     *
     * @param categoryCode the code of the category to filter products by; must not be blank
     * @param page the page number for the pagination; must be 0 or a positive integer, default is 0
     * @param size the number of products per page; must be 1 or greater, default is 12
     * @return a ResponseEntity containing a paginated list of ProductDto objects
     */
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
    /**
     * Retrieves a paginated list of featured products.
     *
     * @param page the page number of products to retrieve*/
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

    // ENDPOINTS ADMIN (TOUS LES PRODUITS)
    /**
     * Retrieves all products, including inactive ones, for admin users.
     * This method is restricted to users with the ADMIN role.
     *
     * @return a {@code ResponseEntity} containing a list of all products as {@code ProductDto}.
     */
    @Operation(summary = "[ADMIN] Obtenir tous les produits (y compris inactifs)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<List<ProductDto>> getAllProductsAdmin() {
        log.info("GET /api/v1/products/admin/all - Fetching all products");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(false) // Inclut les inactifs
                .build();

        Page<ProductDto> products = productService.searchProducts(criteria, PageRequest.of(0, 1000));
        return ResponseEntity.ok(products.getContent());
    }

    /**
     * Retrieves a paginated list of all products, including inactive ones, for administrators.
     *
     * @param page the page number to retrieve, must be zero or greater. Defaults to 0 if not provided.
     * @param size the number of products per page, must be at least 1. Defaults to 50 if not provided.
     * @param sortBy the field by which the results should be sorted. Defaults to NAME if not specified.
     * @param sortDirection the direction of sorting, either ASC (ascending) or DESC (descending). Defaults to ASC if not specified.
     * @return a ResponseEntity containing a Page of ProductDto objects representing the paginated list of products.
     */
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
                .activeOnly(false) // Inclut les inactifs
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductDto> products = productService.searchProducts(criteria, pageable);

        return ResponseEntity.ok(products);
    }

    /**
     * Retrieves statistics of products, including the count of active products,
     * out-of-stock products, and the total number of products. Only accessible
     * by administrators.
     *
     * @return ResponseEntity containing a success response DTO with product
     * statistics data, HTTP status code, and a success message.
     */
    @Operation(summary = "[ADMIN] Obtenir les statistiques des produits")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/stats")
    public ResponseEntity<SuccessResponseDto> getProductStats() {
        log.info("GET /api/v1/products/admin/stats - Fetching product statistics");

        long activeCount = productService.countActiveProducts();
        long outOfStockCount = productService.countOutOfStockProducts();

        Map<String, Object> stats = Map.of(
                "activeProducts", activeCount,
                "outOfStockProducts", outOfStockCount,
                "totalProducts", activeCount + outOfStockCount // Approximation
        );

        return ResponseEntity.ok(
                SuccessResponseDto.of(
                        "Statistiques récupérées avec succès",
                        HttpStatus.OK.value(),
                        stats
                )
        );
    }

    // CRUD ADMIN

    /**
     * Creates a new product in the system.
     *
     * @param productDto the data transfer object containing product details
     *                    to be created. Must be a valid instance.
     * @return a {@code ResponseEntity} containing the created product details
     *         wrapped in a {@code ProductDto} and an HTTP status of CREATED.
     */
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

    /**
     * Updates the details of an existing product.
     *
     * @param id The ID of the product to be updated. Must be a positive integer and is required.
     * @param productDto The updated product details to be applied. Must be valid and is required.
     * @return A ResponseEntity containing the updated product details.
     */
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

    /**
     * Partially updates a product by its ID. This endpoint requires the user to have ADMIN role.
     *
     * @param id the ID of the product to be updated; must be a positive long value
     * @param productDto the partial product details to be updated
     * @return a ResponseEntity containing the updated ProductDto object
     */
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

    /**
     * Deletes a product by performing a soft delete operation, which renders the product inactive.
     *
     * Only accessible to users with the ADMIN role. The soft delete ensures the product data
     * remains in the database but is marked as inactive.
     *
     * @param id the ID of the product to be soft deleted. Must be a positive integer (minimum value: 1).
     * @return a {@code ResponseEntity<ResponseDto>} containing a response status and message confirming
     *         the successful deletion of the product.
     */
    @Operation(summary = "[ADMIN] Supprimer un produit (soft delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto> deleteProduct(
            @Parameter(description = "ID du produit à supprimer", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("DELETE /api/v1/products/{} - Soft deleting product", id);

        productService.deleteProduct(id);

        return ResponseEntity.ok(
                new ResponseDto("200", "Produit supprimé avec succès (devenu inactif)")
        );
    }

    /**
     * Restores a previously deleted product by its ID. This operation is restricted to users with an ADMIN role.
     *
     * @param id the ID of the product to be restored; must be a positive number
     * @return a ResponseEntity containing the restored ProductDto
     */
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

    // GESTION DES IMAGES - ADMIN

    /**
     * Uploads an image for the specified product.
     * This operation is restricted to users with the ADMIN role.
     *
     * @param id the ID of the product for which the image is being uploaded
     *           (must be a positive number, minimum value of 1)
     * @param imageFile the image file to be uploaded for the product
     *                  (must be provided and valid)
     * @return a {@code ResponseEntity<SuccessResponseDto>} containing the
     *         success message, HTTP status, and a map with the URL of the uploaded image
     * @throws IOException if an I/O error occurs during the image upload process
     */
    @Operation(summary = "[ADMIN] Uploader une image pour un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDto> uploadProductImage(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichier image", required = true)
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        log.info("POST /api/v1/products/{}/image - Uploading image: {}",
                id, imageFile.getOriginalFilename());

        String imageUrl = productService.uploadProductImage(id, imageFile);

        return ResponseEntity.ok(
                SuccessResponseDto.of(
                        "Image uploadée avec succès",
                        HttpStatus.OK.value(),
                        Map.of("imageUrl", imageUrl)
                )
        );
    }

    /**
     * Deletes the image of a specified product by its ID.
     *
     * @param id the ID of the product whose image needs to be deleted; must be a positive number.
     * @return a ResponseEntity containing a ResponseDto with a success message.
     */
    @Operation(summary = "[ADMIN] Supprimer l'image d'un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/image")
    public ResponseEntity<ResponseDto> deleteProductImage(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id) {

        log.info("DELETE /api/v1/products/{}/image - Deleting product image", id);

        productService.deleteProductImage(id);

        return ResponseEntity.ok(
                new ResponseDto("200", "Image supprimée avec succès")
        );
    }

    /**
     * Retrieves the bytes of a product image by the given product ID.
     *
     * @param id the ID of the product whose image bytes are to be retrieved. Must be greater than or equal to 1.
     * @return a ResponseEntity containing the bytes of the product image and a content type of image/jpeg.
     */
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


    // GALERIE D'IMAGES - ADMIN

    /**
     * Uploads multiple images for a product and adds them to the product's gallery.
     * This endpoint can only be accessed by users with the ADMIN role.
     *
     * @param id the ID of the product for which the images are being uploaded; must be greater than zero
     * @param imageFiles a list of image files to upload; must not be null or empty
     * @return a ResponseEntity containing a SuccessResponseDto, which includes the status, a message indicating the number
     *         of images uploaded, and a map containing the URLs of the uploaded images
     * @throws IOException if there is an error during file upload
     */
    @Operation(summary = "[ADMIN] Uploader plusieurs images pour un produit")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDto> uploadProductImages(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichiers images", required = true)
            @RequestParam("images") List<MultipartFile> imageFiles) throws IOException {

        log.info("POST /api/v1/products/{}/gallery - Uploading {} images", id, imageFiles.size());

        List<String> imageUrls = productService.uploadProductImages(id, imageFiles);

        return ResponseEntity.ok(
                SuccessResponseDto.of(
                        imageUrls.size() + " images ajoutées à la galerie",
                        HttpStatus.OK.value(),
                        Map.of("imageUrls", imageUrls)
                )
        );
    }

    /**
     * Adds an image to the product gallery.
     * Only accessible by users with ADMIN role.
     *
     * @param id the ID of the product to which the image will be added.
     *           Must be a positive number.
     * @param imageFile the image file to be added to the gallery.
     *                  The file is required and must be provided as
     *                  multipart form data.
     * @return a ResponseEntity containing a SuccessResponseDto
     *         with a success message, HTTP status, and the URL of the added image.
     * @throws IOException if an error occurs during the file processing.
     */
    @Operation(summary = "[ADMIN] Ajouter une image à la galerie")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/gallery/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessResponseDto> addToProductGallery(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @Parameter(description = "Fichier image", required = true)
            @RequestParam("image") MultipartFile imageFile) throws IOException {

        log.info("POST /api/v1/products/{}/gallery/single - Adding image to gallery", id);

        String imageUrl = productService.addToProductGallery(id, imageFile);

        return ResponseEntity.ok(
                SuccessResponseDto.of(
                        "Image ajoutée à la galerie",
                        HttpStatus.OK.value(),
                        Map.of("imageUrl", imageUrl)
                )
        );
    }

    /**
     * Removes an image from the product's gallery.
     *
     * @param productId the ID of the product whose gallery image is to be removed; must be greater than 0
     * @param imageUrl the URL of the image to be removed from the gallery
     * @return a ResponseEntity containing a ResponseDto with status and message indicating the result of the operation
     */
    @Operation(summary = "[ADMIN] Supprimer une image de la galerie")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{productId}/gallery")
    public ResponseEntity<ResponseDto> removeFromProductGallery(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long productId,

            @Parameter(description = "URL de l'image à supprimer", required = true)
            @RequestParam String imageUrl) {

        log.info("DELETE /api/v1/products/{}/gallery - Removing image: {}", productId, imageUrl);

        productService.removeFromProductGallery(productId, imageUrl);

        return ResponseEntity.ok(
                new ResponseDto("200", "Image supprimée de la galerie avec succès")
        );
    }

    /**
     * Reorders the gallery images of a specified product.
     *
     * @param id the ID of the product whose gallery images are to be reordered; must be a positive number.
     * @param imageUrlsInOrder a list of image URLs representing the desired order of the gallery images.
     * @return a ResponseEntity containing a ResponseDto indicating the result of the operation.
     */
    @Operation(summary = "[ADMIN] Réorganiser la galerie d'images")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/gallery/reorder")
    public ResponseEntity<ResponseDto> reorderGalleryImages(
            @Parameter(description = "ID du produit", required = true)
            @PathVariable @Min(1) Long id,

            @RequestBody List<String> imageUrlsInOrder) {

        log.info("PUT /api/v1/products/{}/gallery/reorder - Reordering {} images",
                id, imageUrlsInOrder.size());

        productService.reorderGalleryImages(id, imageUrlsInOrder);

        return ResponseEntity.ok(
                new ResponseDto("200", "Galerie réorganisée avec succès")
        );
    }
}