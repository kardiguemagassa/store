package com.store.store.service.impl;

import com.store.store.dto.ProductDto;
import com.store.store.dto.ProductSearchCriteria;
import com.store.store.entity.Category;
import com.store.store.entity.Product;
import com.store.store.exception.*;
import com.store.store.repository.CategoryRepository;
import com.store.store.repository.ProductRepository;
import com.store.store.service.IProductService;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing products, including their retrieval, creation,
 * updates, and specialized queries such as searching, filtering, and counting. This class
 * interacts primarily with data repositories, file storage services, and caching layers
 * to execute business logic related to products.
 *
 * Fields:
 * - productRepository: Repository for performing database operations on the product entity.
 * - categoryRepository: Repository for interacting with product categories.
 * - fileStorageService: Service for handling file storage, such as product images.
 * - exceptionFactory: Factory for creating custom exceptions in business logic flows.
 * - messageSource: Source for resolving localized messages.
 * - log: Logger instance for capturing and recording application logs.
 *
 * Main features of this class include:
 * - Searching and filtering products based on various criteria.
 * - Pagination and sorting support for product queries.
 * - Managing active, inactive, featured, and sale-related products.
 * - Performing statistical operations such as counting active, inactive, or out-of-stock products.
 * - CRUD operations for products, ensuring validation and data integrity.
 * - Cache management to improve performance and reduce database overhead.
 *
 * Transaction Management:
 * - Read-only transactions are used where data retrieval occurs to optimize resources.
 * - Write transactions are used when creating or updating products, with appropriate cache evictions.
 *
 * This class is designed with caching and business exception management to ensure robust and
 * scalable operations in product management workflows.
 *
 *  @author Kardigué
 *  * @version 3.0 - Production Ready
 *  * @since 2025-10-27
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageServiceImpl fileStorageService;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;


    // RECHERCHE ET LECTURE

    /**
     * Searches for products based on the provided search criteria and pagination settings.
     *
     * @param criteria the criteria containing filters for the product search, such as name, category, price range, or availability
     * @param pageable the pagination information specifying the page number, size, and sorting order
     * @return a page containing the products that match the search criteria, transformed into DTOs
     * @throws BusinessException if an error occurs during the search process
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        try {
            log.info("Searching products with criteria: {}", criteria);

            Specification<Product> spec = buildSpecification(criteria);
            Page<Product> products = productRepository.findAll(spec, pageable);

            log.info("Found {} products", products.getTotalElements());
            return products.map(this::transformToDTO);

        } catch (Exception e) {
            log.error("Error searching products with criteria: {}", criteria, e);
            throw exceptionFactory.businessError("Failed to search products");
        }
    }

    /**
     * Builds a specification for filtering and querying products based on the provided search criteria.
     *
     * @param criteria the product search criteria containing filters such as category, price range, stock availability,
     *                 active status, and search query for name or description
     * @return a Specification object for querying products based on the specified criteria
     */
    private Specification<Product> buildSpecification(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtre par statut actif
            if (Boolean.TRUE.equals(criteria.activeOnly())) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            // Filtre par catégorie
            if (criteria.categoryCode() != null && !criteria.categoryCode().isEmpty()) {
                predicates.add(cb.equal(root.get("category").get("code"), criteria.categoryCode()));
            }

            // Filtre par prix minimum
            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
            }

            // Filtre par prix maximum
            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
            }

            // Filtre par stock
            if (Boolean.TRUE.equals(criteria.inStockOnly())) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }

            // Recherche texte
            if (criteria.searchQuery() != null && !criteria.searchQuery().isEmpty()) {
                String searchPattern = "%" + criteria.searchQuery().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descriptionPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(namePredicate, descriptionPredicate));
            }

            // Tri personnalisé si spécifié
            if (query != null && criteria.sortBy() != null) {
                applySorting(query, cb, root, criteria);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Applies sorting to a CriteriaQuery based on the provided ProductSearchCriteria.
     *
     * @param query    the CriteriaQuery to which the sorting will be applied
     * @param cb       the CriteriaBuilder used for constructing query elements
     * @param root     the root entity in the query representing the Product table
     * @param criteria the search criteria containing the sort field and direction
     */
    private void applySorting(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<Product> root,
            ProductSearchCriteria criteria) {

        Order order = criteria.sortDirection() == ProductSearchCriteria.SortDirection.DESC ?
                cb.desc(getSortExpression(root, criteria.sortBy())) :
                cb.asc(getSortExpression(root, criteria.sortBy()));

        query.orderBy(order);
    }

    /**
     * Generates a sorting expression based on the given sort criteria and root entity.
     *
     * @param root the root entity of the query from which the sort expression will be derived
     * @param sortBy the sort criteria used to determine the field for sorting
     * @return an expression representing the sorting field determined by the specified sort criteria
     */
    private Expression<?> getSortExpression(Root<Product> root, ProductSearchCriteria.SortBy sortBy) {
        return switch (sortBy) {
            case PRICE -> root.get("price");
            case POPULARITY -> root.get("popularity");
            case CREATED_DATE -> root.get("createdAt");
            default -> root.get("name"); // NAME par défaut
        };
    }

    // MÉTHODES DE LECTURE ESSENTIELLES

    /**
     * Retrieves a list of all products from the database and transforms them into Product DTOs.
     * The result is cached unless it is an empty list.
     *
     * @return a list of ProductDto objects representing all products,
     *         or an empty list if no products are found.
     * @throws BusinessException if an error occurs while fetching the products.
     */
    @Cacheable(value = "products", unless = "#result.isEmpty()")
    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        try {
            log.info("Fetching all products from database");
            List<Product> products = productRepository.findAllWithCategory();
            log.info("Found {} products", products.size());
            return products.stream()
                    .map(this::transformToDTO)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching products", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.failed")
            );
        }
    }

    /**
     * Retrieves a paginated list of all products and transforms them into Product DTOs.
     *
     * @param pageable the pagination information specifying the page number, size, and sorting order
     * @return a Page object containing the Product DTOs for the requested page
     * @throws BusinessException if an error occurs while fetching the products
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        try {
            log.info("Fetching products with pagination: page {}, size {}",
                    pageable.getPageNumber(), pageable.getPageSize());

            Page<Product> productPage = productRepository.findAllWithCategory(pageable);
            log.info("Found {} products on page {}", productPage.getContent().size(), pageable.getPageNumber());
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while fetching paginated products", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.paginated.failed")
            );
        }
    }

    /**
     * Retrieves a product by its unique identifier.
     *
     * @param id the unique identifier of the product to be retrieved
     * @return a {@code ProductDto} representing the product information
     * @throws IllegalArgumentException if the provided {@code id} is invalid
     * @throws ResourceNotFoundException if no product is found for the given {@code id}
     * @throws BusinessException if a database access error occurs during the operation
     */
    @Cacheable(value = "product", key = "#id")
    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        try {
            log.info("Fetching product by ID: {}", id);
            validateProductId(id);

            Product product = productRepository.findByIdWithCategory(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Product", "id", id.toString()));

            log.info("Product found: {} (ID: {})", product.getName(), id);
            return transformToDTO(product);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.byId.failed")
            );
        }
    }

    /**
     * Fetches a paginated list of active products.
     *
     * @param pageable a Pageable object containing pagination information such as page number and size
     * @return a Page containing ProductDto objects representing active products
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getActiveProducts(Pageable pageable) {
        log.info("Fetching active products with pagination");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder().activeOnly(true).build();

        return searchProducts(criteria, pageable);
    }

    /**
     * Retrieves a paginated list of inactive products.
     *
     * @param pageable the pagination and sorting information
     * @return a page containing inactive products wrapped in ProductDto
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getInactiveProducts(Pageable pageable) {
        log.info("Fetching inactive products with pagination");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(false)  // ← On veut les inactifs
                .build();

        // Utiliser une spec personnalisée pour les inactifs
        Specification<Product> spec = buildInactiveProductsSpecification();
        Page<Product> products = productRepository.findAll(spec, pageable);

        log.info("Found {} inactive products", products.getTotalElements());
        return products.map(this::transformToDTO);
    }

    /**
     * Counts the total number of inactive products in the system.
     *
     * @return the total count of inactive products as a long value
     */
    @Override
    @Transactional(readOnly = true)
    public long countInactiveProducts() {
        long count = productRepository.countInactiveProducts();
        log.info("Total inactive products: {}", count);
        return count;
    }

    // MÉTHODES MÉTIER SPÉCIALISÉES

    /**
     * Retrieves a paginated list of featured products.
     * Featured products are determined based on predefined criteria such as availability, stock status,
     * and popularity.
     *
     * @param pageable the pagination and sorting information
     * @return a paginated list of featured product details as {@code Page<ProductDto>}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getFeaturedProducts(Pageable pageable) {
        log.info("Fetching featured products");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(true)
                .inStockOnly(true)
                .sortBy(ProductSearchCriteria.SortBy.POPULARITY)
                .sortDirection(ProductSearchCriteria.SortDirection.DESC)
                .build();

        return searchProducts(criteria, pageable);
    }

    /**
     * Retrieves a paginated list of products that are currently on sale.
     *
     * @param pageable the pagination and sorting information
     * @return a paginated list of products on sale wrapped in a Page of ProductDto
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getProductsOnSale(Pageable pageable) {
        log.info("Fetching products on sale");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .activeOnly(true)
                .sortBy(ProductSearchCriteria.SortBy.POPULARITY)
                .sortDirection(ProductSearchCriteria.SortDirection.DESC)
                .build();

        return searchProducts(criteria, pageable);
    }

    /**
     * Counts the number of products that are currently active in the system.
     *
     * @return the total number of active products
     */
    @Override
    @Transactional(readOnly = true)
    public long countActiveProducts() {
        return productRepository.countActiveProducts();
    }

    /**
     * Counts the number of products that are currently out of stock.
     * This method interacts with the product repository to determine the count.
     *
     * @return the total number of products that are out of stock
     */
    @Override
    @Transactional(readOnly = true)
    public long countOutOfStockProducts() {
        return productRepository.countOutOfStockProducts();
    }

    // CRUD PRINCIPAL
    /**
     * Creates a new product based on the provided ProductDto. Validates the input,
     * ensures product uniqueness by name, associates it with a category, and saves
     * the product in the database.
     *
     * @param productDto the ProductDto object containing details of the product to be created,
     *                   such as name, description, price, and category information
     * @return the created ProductDto object containing the saved product details, including its ID
     * @throws BusinessException if a product with the same name already exists or other business rules are violated
     * @throws ResourceNotFoundException if the specified category for the product does not exist
     * @throws DataAccessException if any database errors occur while saving the product
     */
    @CacheEvict(value = {"products", "productsByCategory"}, allEntries = true)
    @Transactional
    @Override
    public ProductDto createProduct(ProductDto productDto) {
        try {
            log.info("Creating new product: {}", productDto.getName());
            validateProductForCreation(productDto);

            if (productRepository.existsByNameIgnoreCase(productDto.getName())) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.product.already.exists", productDto.getName())
                );
            }

            Category category = getCategoryById(productDto.getCategoryId());
            Product product = createProductEntity(productDto, category);
            Product savedProduct = productRepository.save(product);

            log.info("Product created successfully with ID: {}", savedProduct.getId());
            return transformToDTO(savedProduct);

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while creating product", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.create.failed")
            );
        }
    }

    /**
     * Updates an existing product with the specified ID using the provided product data.
     * Performs validation on the product ID and product data before updating.
     * Evicts related caches after a successful update operation.
     *
     * @param id the ID of the product to be updated
     * @param productDto a data transfer object containing the updated product information
     * @return the updated product as a data transfer object
     * @throws ResourceNotFoundException if the product with the given ID does not exist
     * @throws BusinessException if any business validation fails
     * @throws DataAccessException if a database error occurs during the update
     */
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#id")
    @Transactional
    @Override
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        try {
            log.info("Updating product ID: {}", id);
            validateProductId(id);
            validateProductForUpdate(productDto);

            Product existingProduct = getProductEntityById(id);
            validateProductNameUniqueness(existingProduct, productDto);
            updateProductCategory(existingProduct, productDto);
            updateProductFields(existingProduct, productDto);

            Product updatedProduct = productRepository.save(existingProduct);
            log.info("Product updated successfully: {}", id);
            return transformToDTO(updatedProduct);

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while updating product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.update.failed")
            );
        }
    }

    /**
     * Soft deletes a product by its ID. The product is marked as inactive instead of being
     * permanently removed from the database.
     * This method evicts the associated cache entries for the product and related lists.
     *
     * @param id The unique identifier of the product to be deleted.
     *           Must not be null or invalid.
     * @throws IllegalArgumentException If the given product ID is null or invalid.
     * @throws ResourceNotFoundException If no product with the specified ID exists.
     * @throws BusinessException If an error occurs during the delete operation.
     */
    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = {"products", "productsByCategory"}, allEntries = true)
    })
    @Transactional
    @Override
    public void deleteProduct(Long id) {
        try {
            log.info("Soft deleting product ID: {}", id);
            validateProductId(id);

            Product product = getProductEntityById(id);
            product.setIsActive(false);
            productRepository.save(product);

            log.info("Product soft deleted successfully: {}", id);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while soft deleting product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.delete.failed")
            );
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "product", key = "#id"),
            @CacheEvict(value = {"products", "productsByCategory"}, allEntries = true)
    })
    @Transactional
    @Override
    public ProductDto restoreProduct(Long id) {
        try {
            log.info("Restoring product ID: {}", id);
            validateProductId(id);

            Product product = getProductEntityById(id);
            product.setIsActive(true);
            Product savedProduct = productRepository.save(product);

            log.info("Product restored successfully: {}", id);
            return transformToDTO(savedProduct);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while restoring product ID: {}", id, e);
            throw exceptionFactory.businessError("Failed to restore product");
        }
    }

    // GESTION DES IMAGES
    /**
     * Uploads an image for a specific product. Validates the product ID, stores the uploaded image,
     * deletes the existing image (if any), updates the product record with the new image URL, and
     * returns the URL of the newly stored image.
     *
     * @param productId the unique identifier of the product for which the image is being uploaded
     * @param imageFile the multipart file object containing the image to be uploaded
     * @return the URL of the uploaded image
     * @throws IOException if an error occurs while storing the image file
     * @throws ResourceNotFoundException if the product with the given ID does not exist
     * @throws ValidationException if the product ID validation fails
     */
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    @Override
    public String uploadProductImage(Long productId, MultipartFile imageFile) throws IOException {
        try {
            log.info("Uploading image for product ID: {}", productId);
            validateProductId(productId);

            Product product = getProductEntityById(productId);
            String imageUrl = fileStorageService.storeProductImage(imageFile);
            deleteProductImage(product);

            product.setImageUrl(imageUrl);
            productRepository.save(product);

            log.info("Image uploaded successfully for product ID: {} -> {}", productId, imageUrl);
            return imageUrl;

        } catch (ResourceNotFoundException | ValidationException | IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading image for product ID: {}", productId, e);
            throw exceptionFactory.fileStorageError(
                    getLocalizedMessage("error.product.image.save.failed"), e
            );
        }
    }

    /**
     * Deletes the image associated with a specific product by its ID. This method
     * ensures that the image reference in the product's record is removed and updates
     * the database accordingly. It also evicts related cache entries to maintain consistency.
     *
     * @param productId the ID of the product whose image needs to be deleted
     * @throws ResourceNotFoundException if the product with the specified ID does not exist
     * @throws DataAccessException if there is a database-related error during the operation
     */
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    @Override
    public void deleteProductImage(Long productId) {
        try {
            log.info("Deleting image for product ID: {}", productId);
            validateProductId(productId);

            Product product = getProductEntityById(productId);
            deleteProductImage(product);
            product.setImageUrl(null);
            productRepository.save(product);

            log.info("Product image deleted successfully: {}", productId);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while deleting product image ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.image.delete.failed")
            );
        }
    }

    /**
     * Retrieves the image bytes for a specified product using its unique identifier.
     *
     * @param productId The unique identifier of the product whose image is to be fetched.
     * @return A byte array representing the product image.
     * @throws IllegalArgumentException if the productId is invalid.
     * @throws ResourceNotFoundException if the product image URL or the image file does not exist.
     * @throws FileStorageException if an error occurs while reading the image file.
     */
    @Override
    public byte[] getProductImageBytes(Long productId) {
        try {
            log.info("Fetching image bytes for product ID: {}", productId);
            validateProductId(productId);

            Product product = getProductEntityById(productId);

            if (product.getImageUrl() == null) {
                throw exceptionFactory.resourceNotFound("Image non trouvée pour le produit: " + productId);
            }

            String fileName = extractFileNameFromUrl(product.getImageUrl());
            Path imagePath = Paths.get("uploads/products").resolve(fileName);

            if (!Files.exists(imagePath)) {
                throw exceptionFactory.resourceNotFound("Fichier image non trouvé: " + fileName);
            }

            return Files.readAllBytes(imagePath);

        } catch (IOException e) {
            log.error("Error reading image file for product ID: {}", productId, e);
            throw exceptionFactory.fileStorageError(
                    getLocalizedMessage("error.product.image.read.failed"), e
            );
        }
    }

    /**
     * Uploads image files for a specific product, stores them, and associates the stored image URLs
     * with the product in the database. Invalid product IDs or empty image files will result in
     * validation errors. The uploaded images' URLs are returned as a list.
     *
     * @param productId The ID of the product to which the images will be associated.
     * @param imageFiles A list of {@link MultipartFile} objects representing the images
     *                   to be uploaded.
     * @return A list of URLs representing the stored images.
     * @throws IOException If an input/output error occurs while processing the image files.
     * @throws ValidationException If the product ID is invalid or the list of image files is empty.
     * @throws DataAccessException If there is an error accessing the database during the process.
     */
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    @Override
    public List<String> uploadProductImages(Long productId, List<MultipartFile> imageFiles) throws IOException {
        try {
            log.info("Uploading {} images for product ID: {}", imageFiles.size(), productId);
            validateProductId(productId);

            if (imageFiles.isEmpty()) {
                throw exceptionFactory.validationError("images",
                        getLocalizedMessage("validation.product.images.required"));
            }

            Product product = getProductEntityById(productId);
            List<String> imageUrls = new ArrayList<>();

            for (MultipartFile imageFile : imageFiles) {
                String imageUrl = fileStorageService.storeProductImage(imageFile);
                imageUrls.add(imageUrl);
            }

            if (product.getGalleryImages() == null) {
                product.setGalleryImages(new ArrayList<>());
            }
            product.getGalleryImages().addAll(imageUrls);
            productRepository.save(product);

            log.info("{} images uploaded successfully for product ID: {}", imageUrls.size(), productId);
            return imageUrls;

        } catch (ValidationException | IOException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while uploading multiple images for product ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.images.upload.failed")
            );
        }
    }

    // GALERIE D'IMAGES
    /**
     * Adds an image to the product's gallery by storing the image and associating it
     * with the specified product.
     *
     * @param productId the ID of the product to which the image is to be added
     * @param imageFile the image file to be added to the product's gallery
     * @return the URL of the stored image
     * @throws IOException if an error occurs while storing the image file
     * @throws ValidationException if the provided product ID is invalid
     * @throws DataAccessException if a database error occurs while saving the product
     */
    @Override
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    public String addToProductGallery(Long productId, MultipartFile imageFile) throws IOException {
        try {
            log.info("Adding image to gallery for product ID: {}", productId);
            validateProductId(productId);

            Product product = getProductEntityById(productId);
            String imageUrl = fileStorageService.storeProductImage(imageFile);

            product.addGalleryImage(imageUrl);
            productRepository.save(product);

            log.info("Image added to gallery for product {}: {}", productId, imageUrl);
            return imageUrl;

        } catch (ValidationException | IOException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while adding image to gallery for product ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.gallery.add.failed")
            );
        }
    }

    /**
     * Removes an image from the product gallery for a specified product.
     *
     * @param productId the ID of the product from which the image is to be removed
     * @param imageUrl the URL of the image to be removed from the product gallery
     */
    @Override
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    public void removeFromProductGallery(Long productId, String imageUrl) {
        try {
            log.info("Removing image from gallery for product ID: {} - {}", productId, imageUrl);
            validateProductId(productId);

            Product product = getProductEntityById(productId);

            if (product.removeGalleryImage(imageUrl)) {
                productRepository.save(product);
                fileStorageService.deleteProductImage(imageUrl);
                log.info("Image removed from gallery for product {}: {}", productId, imageUrl);
            } else {
                log.warn("Image not found in gallery for product {}: {}", productId, imageUrl);
            }

        } catch (DataAccessException e) {
            log.error("Database error while removing image from gallery for product ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.gallery.remove.failed")
            );
        }
    }

    /**
     * Reorders the gallery images of a given product based on the provided list of image URLs.
     *
     * @param productId The unique identifier of the product whose gallery images are to be reordered.
     * @param imageUrlsInOrder The list of image URLs representing the new order of the gallery images.
     *                          The list must contain all existing URLs in the product's gallery.
     * @throws IllegalArgumentException If the provided productId is null or invalid.
     * @throws ValidationException If the provided URLs do not match the current gallery images.
     * @throws BusinessException If a database error occurs while saving the reordered gallery.
     */
    @Override
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    public void reorderGalleryImages(Long productId, List<String> imageUrlsInOrder) {
        try {
            log.info("Reordering gallery images for product ID: {}", productId);
            validateProductId(productId);

            Product product = getProductEntityById(productId);

            if (product.getGalleryImages() != null &&
                    new HashSet<>(product.getGalleryImages()).containsAll(imageUrlsInOrder)) {

                product.setGalleryImages(new ArrayList<>(imageUrlsInOrder));
                productRepository.save(product);
                log.info("Gallery reordered for product {}: {} images", productId, imageUrlsInOrder.size());
            } else {
                throw exceptionFactory.validationError("galleryImages",
                        "Les URLs fournies ne correspondent pas à la galerie actuelle");
            }

        } catch (DataAccessException e) {
            log.error("Database error while reordering gallery for product ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.gallery.reorder.failed")
            );
        }
    }


    // MÉTHODES PRIVÉES

    /**
     * Retrieves a Product entity by its unique identifier.
     *
     * @param id the unique identifier of the Product to retrieve
     * @return the Product entity corresponding to the given id
     * @throws ResourceNotFoundException if no Product entity with the given id is found
     */
    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("Product", "id", id.toString()));
    }

    /**
     * Retrieves a Category entity based on the provided category ID.
     *
     * @param categoryId the ID of the category to be retrieved
     * @return the Category entity corresponding to the given ID
     * @throws ResourceNotFoundException if no category is found with the specified ID
     */
    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound(
                        "Category", "id", categoryId.toString()
                ));
    }

    /**
     * Validates that the product name provided in the productDto is unique.
     * If the name is not unique and already exists in the repository, a business error is thrown.
     *
     * @param existingProduct the existing product being updated
     * @param productDto      the product data transfer object containing the new product details
     */
    private void validateProductNameUniqueness(Product existingProduct, ProductDto productDto) {
        if (!existingProduct.getName().equalsIgnoreCase(productDto.getName()) &&
                productRepository.existsByNameIgnoreCase(productDto.getName())) {
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.already.exists", productDto.getName())
            );
        }
    }

    /**
     * Updates the category of an existing product based on the provided product DTO.
     * If the category ID in the product DTO differs from the current category ID of
     * the existing product, the category is updated.
     *
     * @param existingProduct the existing product whose category is to be updated
     * @param productDto the data transfer object containing the updated category information
     */
    private void updateProductCategory(Product existingProduct, ProductDto productDto) {
        if (productDto.getCategoryId() != null &&
                !productDto.getCategoryId().equals(existingProduct.getCategory().getCategoryId())) {

            Category newCategory = getCategoryById(productDto.getCategoryId());
            existingProduct.setCategory(newCategory);
            log.info("Category updated from {} to {}",
                    existingProduct.getCategory().getName(),
                    newCategory.getName());
        }
    }

    /**
     * Updates the fields of an existing product with the data from the provided product DTO.
     *
     * @param existingProduct the product entity to be updated
     * @param productDto the product data transfer object containing updated field values
     */
    private void updateProductFields(Product existingProduct, ProductDto productDto) {
        existingProduct.setName(productDto.getName());
        existingProduct.setDescription(productDto.getDescription());
        existingProduct.setPrice(productDto.getPrice());
        existingProduct.setImageUrl(productDto.getImageUrl());

        if (productDto.getGalleryImages() != null) {
            existingProduct.setGalleryImages(new ArrayList<>(productDto.getGalleryImages()));
        }
    }

    /**
     * Deletes the product image associated with the given product, if an image URL exists.
     *
     * @param product the product whose associated image is to be deleted
     */
    private void deleteProductImage(Product product) {
        if (product.getImageUrl() != null) {
            fileStorageService.deleteProductImage(product.getImageUrl());
        }
    }

    /**
     * Extracts the file name from the given URL.
     *
     * @param imageUrl the URL of the image
     * @return the extracted file name from the URL
     */
    private String extractFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    /**
     * Validates the provided product ID to ensure it is not null and is greater than 0.
     *
     * @param id the product ID to be validated
     * @throws IllegalArgumentException if the product ID is null or less than or equal to 0
     */
    private void validateProductId(Long id) {
        if (id == null || id <= 0) {
            throw exceptionFactory.validationError("productId",
                    getLocalizedMessage("validation.product.id.invalid"));
        }
    }

    /**
     * Validates the given ProductDto object to ensure it meets the required criteria
     * for product creation. Throws a validation error exception with appropriate
     * messages if any of the validations fail.
     *
     * @param productDto The product data transfer object to be validated.
     *                   Must not be null and must contain valid name, price, and
     *                   category ID attributes. Additional constraints include
     *                   length limits for name and description fields,
     *                   and price must be greater than zero.
     */
    private void validateProductForCreation(ProductDto productDto) {
        if (productDto == null) {
            throw exceptionFactory.validationError("productDto",
                    getLocalizedMessage("validation.product.create.required"));
        }
        if (productDto.getName() == null || productDto.getName().trim().isEmpty()) {
            throw exceptionFactory.validationError("name",
                    getLocalizedMessage("validation.product.name.required"));
        }
        if (productDto.getPrice() == null || productDto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw exceptionFactory.validationError("price",
                    getLocalizedMessage("validation.product.price.invalid"));
        }
        if (productDto.getCategoryId() == null) {
            throw exceptionFactory.validationError("categoryId",
                    getLocalizedMessage("validation.product.categoryId.required"));
        }
        if (productDto.getName().length() > 250) {
            throw exceptionFactory.validationError("name",
                    getLocalizedMessage("validation.product.name.tooLong", 250));
        }
        if (productDto.getDescription() != null && productDto.getDescription().length() > 500) {
            throw exceptionFactory.validationError("description",
                    getLocalizedMessage("validation.product.description.tooLong", 500));
        }
    }

    /**
     * Validates the provided product data for update operations.
     *
     * @param productDto the product data to be validated. It must not be null, and its productId must be specified.
     *                   The method also performs creation validations on the product data.
     * @throws ValidationException if productDto is null or its productId is missing.
     */
    private void validateProductForUpdate(ProductDto productDto) {
        if (productDto == null) {
            throw exceptionFactory.validationError("productDto",
                    getLocalizedMessage("validation.product.update.required"));
        }

        if (productDto.getProductId() == null) {
            throw exceptionFactory.validationError("productId",
                    getLocalizedMessage("validation.product.id.requiredForUpdate"));
        }

        validateProductForCreation(productDto);
    }

    /**
     * Builds a specification to filter inactive products.
     * The specification checks whether the "isActive" property of a product is set to false,
     * representing soft-deleted products.
     *
     * @return a {@link Specification} instance for filtering inactive products
     */
    private Specification<Product> buildInactiveProductsSpecification() {
        return (root, query, cb) -> {
            // isActive = false (produits soft deleted)
            return cb.isFalse(root.get("isActive"));
        };
    }

    // MAPPER

    /**
     * Creates a Product entity using the provided ProductDto and Category.
     *
     * @param productDto the data transfer object containing product details
     * @param category the category to associate with the product
     * @return a new Product entity populated with the specified details
     */
    private Product createProductEntity(ProductDto productDto, Category category) {
        Product product = new Product();
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setPopularity(0);
        product.setImageUrl(productDto.getImageUrl());
        product.setCategory(category);
        return product;
    }

    /**
     * Transforms a Product entity to its corresponding ProductDto representation.
     *
     * @param product the Product entity to be transformed
     * @return the corresponding ProductDto containing the transformed data
     */
    private ProductDto transformToDTO(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setProductId(product.getId());
        productDto.setName(product.getName());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setPopularity(product.getPopularity());
        productDto.setStockQuantity(product.getStockQuantity());
        productDto.setImageUrl(product.getImageUrl());
        productDto.setIsActive(product.getIsActive());
        productDto.setGalleryImages(product.getGalleryImages());

        if (product.getCategory() != null) {
            productDto.setCategoryId(product.getCategory().getCategoryId());
            productDto.setCategoryCode(product.getCategory().getCode());
            productDto.setCategoryName(product.getCategory().getName());
            productDto.setCategoryIcon(product.getCategory().getIcon());
        }

        return productDto;
    }

    /**
     * Retrieves a localized message based on the provided message code and arguments.
     * This method uses the current locale from the LocaleContextHolder to fetch
     * the appropriate message from the message source.
     *
     * @param code the message code to look up, which acts as a key in the message source
     * @param args the arguments to be used within the message, which can be placeholders
     *             in the message string
     * @return the localized message string for the given code and arguments
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}