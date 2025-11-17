package com.store.store.service.impl;

import com.store.store.dto.product.ProductDto;
import com.store.store.dto.product.ProductSearchCriteria;
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
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageServiceImpl fileStorageService;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    // RECHERCHE ET FILTRAGE

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


    private Specification<Product> buildSpecification(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtre par statut actif
            if (Boolean.TRUE.equals(criteria.activeOnly())) {predicates.add(cb.isTrue(root.get("isActive")));}

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

    private void applySorting(CriteriaQuery<?> query, CriteriaBuilder cb, Root<Product> root, ProductSearchCriteria criteria) {
        Order order = criteria.sortDirection() == ProductSearchCriteria.SortDirection.DESC ?
                cb.desc(getSortExpression(root, criteria.sortBy())) :
                cb.asc(getSortExpression(root, criteria.sortBy()));

        query.orderBy(order);
    }

    private Expression<?> getSortExpression(Root<Product> root, ProductSearchCriteria.SortBy sortBy) {
        return switch (sortBy) {
            case PRICE -> root.get("price");
            case POPULARITY -> root.get("popularity");
            case CREATED_DATE -> root.get("createdAt");
            default -> root.get("name");
        };
    }

    // LECTURE DES PRODUITS

    @Cacheable(value = "products", unless = "#result.isEmpty()")
    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        try {
            List<Product> products = productRepository.findAllWithCategory();
            log.info("Found {} products", products.size());
            return products.stream().map(this::transformToDTO).collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching products", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.product.fetch.failed"));
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getAllProducts(Pageable pageable) {
        try {
            log.info("Fetching products with pagination: page {}, size {}", pageable.getPageNumber(), pageable.getPageSize());

            Page<Product> productPage = productRepository.findAllWithCategory(pageable);
            log.info("Found {} products on page {}", productPage.getContent().size(), pageable.getPageNumber());
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while fetching paginated products", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.product.fetch.paginated.failed")
            );
        }
    }

    @Cacheable(value = "product", key = "#id")
    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        try {
            log.info("Fetching product by ID: {}", id);
            validateProductId(id);

            Product product = productRepository.findByIdWithCategory(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Product", "id", id.toString()));

            log.info("Product found: {} (ID: {})", product.getName(), id);
            return transformToDTO(product);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching product ID: {}", id, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.product.fetch.byId.failed"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getActiveProducts(Pageable pageable) {
        log.info("Fetching active products with pagination");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder().activeOnly(true).build();

        return searchProducts(criteria, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> getInactiveProducts(Pageable pageable) {
        log.info("Fetching inactive products with pagination");

        ProductSearchCriteria criteria = ProductSearchCriteria.builder().activeOnly(false).build();

        Specification<Product> spec = buildInactiveProductsSpecification();
        Page<Product> products = productRepository.findAll(spec, pageable);

        log.info("Found {} inactive products", products.getTotalElements());
        return products.map(this::transformToDTO);
    }


    @Override
    @Transactional(readOnly = true)
    public long countInactiveProducts() {
        long count = productRepository.countInactiveProducts();
        log.info("Total inactive products: {}", count);
        return count;
    }

    // MÉTHODES MÉTIER SPÉCIALISÉES

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

    @Override
    @Transactional(readOnly = true)
    public long countActiveProducts() {
        return productRepository.countActiveProducts();
    }

    @Override
    @Transactional(readOnly = true)
    public long countOutOfStockProducts() {
        return productRepository.countOutOfStockProducts();
    }

    // CRUD PRINCIPAL

    @CacheEvict(value = {"products", "productsByCategory"}, allEntries = true)
    @Transactional
    @Override
    public ProductDto createProduct(ProductDto productDto) {
        try {
            log.info("Creating new product: {}", productDto.getName());
            validateProductForCreation(productDto);

            if (productRepository.existsByNameIgnoreCase(productDto.getName())) {
                throw exceptionFactory.businessError(messageService.getMessage("error.product.already.exists", productDto.getName()));
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
            throw exceptionFactory.businessError(messageService.getMessage("error.product.create.failed"));
        }
    }

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
            throw exceptionFactory.businessError(messageService.getMessage("error.product.update.failed"));
        }
    }

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
            throw exceptionFactory.businessError(messageService.getMessage("error.product.delete.failed")
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

    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    @Override
    public String uploadProductImage(Long productId, MultipartFile imageFile) throws IOException {
        try {
            log.info("Uploading image for product ID: {}", productId);
            validateProductId(productId);

            Product product = getProductEntityById(productId);
            //String imageUrl = fileStorageService.storeProductImage(imageFile);
            String imageUrl = fileStorageService.storeGalleryImage(imageFile);
            deleteProductImage(product);

            product.setImageUrl(imageUrl);
            productRepository.save(product);

            log.info("Image uploaded successfully for product ID: {} -> {}", productId, imageUrl);
            return imageUrl;

        } catch (ResourceNotFoundException | ValidationException | IOException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error uploading image for product ID: {}", productId, e);
            throw exceptionFactory.fileStorageError(messageService.getMessage("error.product.image.save.failed"), e);
        }
    }

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
            throw exceptionFactory.businessError(messageService.getMessage("error.product.image.delete.failed"));
        }
    }

    @Override
    public byte[] getProductImageBytes(Long productId) {
        try {
            log.info("Fetching image bytes for product ID: {}", productId);

            // 1. Validation de l'ID
            validateProductId(productId);

            // 2. Récupération du produit
            Product product = getProductEntityById(productId);

            // 3. Vérification de l'URL image
            if (product.getImageUrl() == null) {
                log.warn("No image URL found for product ID: {}", productId);
                throw exceptionFactory.resourceNotFoundById("ProductImage", productId);
            }

            // 4. Extraction du nom de fichier
            String fileName = extractFileNameFromUrl(product.getImageUrl());
            Path imagePath = Paths.get("uploads/products").resolve(fileName);

            // 5. Vérification de l'existence physique
            if (!Files.exists(imagePath)) {
                log.error("Physical image file not found: {}", imagePath);
                throw exceptionFactory.resourceNotFoundById("ImageFile", fileName);
            }

            // 6. Lecture et retour
            byte[] imageBytes = Files.readAllBytes(imagePath);
            log.info("Successfully retrieved {} bytes for product ID: {}", imageBytes.length, productId);

            return imageBytes;

        } catch (ResourceNotFoundException | ValidationException e) {
            // Re-throw business exceptions
            throw e;
        } catch (IOException e) {
            log.error("IO error reading image file for product ID: {}", productId, e);
            throw exceptionFactory.fileStorageError(messageService.getMessage("error.product.image.read.failed"), e);
        }
    }

    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    @Override
    public List<String> uploadProductImages(Long productId, List<MultipartFile> imageFiles) throws IOException {
        try {
            log.info("Uploading {} images for product ID: {}", imageFiles.size(), productId);
            validateProductId(productId);

            if (imageFiles.isEmpty()) {
                throw exceptionFactory.validationError("images", messageService.getMessage("validation.product.images.required"));
            }

            Product product = getProductEntityById(productId);
            List<String> imageUrls = new ArrayList<>();

            for (MultipartFile imageFile : imageFiles) {
                //String imageUrl = fileStorageService.storeProductImage(imageFile);
                String imageUrl = fileStorageService.storeGalleryImage(imageFile);
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
            throw exceptionFactory.businessError(messageService.getMessage("error.product.images.upload.failed"));
        }
    }

    // GALERIE D'IMAGES

    @Override
    @CacheEvict(value = {"product", "products", "productsByCategory"}, key = "#productId")
    @Transactional
    public String addToProductGallery(Long productId, MultipartFile imageFile) throws IOException {
        try {
            log.info("Adding image to gallery for product ID: {}", productId);
            validateProductId(productId);

            Product product = getProductEntityById(productId);
            //String imageUrl = fileStorageService.storeProductImage(imageFile);
            String imageUrl = fileStorageService.storeGalleryImage(imageFile);

            product.addGalleryImage(imageUrl);
            productRepository.save(product);

            log.info("Image added to gallery for product {}: {}", productId, imageUrl);
            return imageUrl;

        } catch (ValidationException | IOException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while adding image to gallery for product ID: {}", productId, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.product.gallery.add.failed"));
        }
    }

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
            throw exceptionFactory.businessError(messageService.getMessage("error.product.gallery.remove.failed"));
        }
    }

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
            throw exceptionFactory.businessError(messageService.getMessage("error.product.gallery.reorder.failed"));
        }
    }

    // MÉTHODES PRIVÉES - UTILITAIRES

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("Product", "id", id.toString()));
    }


    private Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> exceptionFactory.resourceNotFound("Category", "id", categoryId.toString()));
    }

    private void validateProductNameUniqueness(Product existingProduct, ProductDto productDto) {
        if (!existingProduct.getName().equalsIgnoreCase(productDto.getName()) &&
                productRepository.existsByNameIgnoreCase(productDto.getName())) {
            throw exceptionFactory.businessError(
                    messageService.getMessage("error.product.already.exists", productDto.getName())
            );
        }
    }

    private void updateProductCategory(Product existingProduct, ProductDto productDto) {
        if (productDto.getCategoryId() != null &&
                !productDto.getCategoryId().equals(existingProduct.getCategory().getCategoryId())) {

            Category newCategory = getCategoryById(productDto.getCategoryId());
            existingProduct.setCategory(newCategory);
            log.info("Category updated from {} to {}", existingProduct.getCategory().getName(), newCategory.getName());
        }
    }

    private void updateProductFields(Product existingProduct, ProductDto productDto) {
        existingProduct.setName(productDto.getName());
        existingProduct.setDescription(productDto.getDescription());
        existingProduct.setPrice(productDto.getPrice());
        existingProduct.setImageUrl(productDto.getImageUrl());

        if (productDto.getGalleryImages() != null) {
            existingProduct.setGalleryImages(new ArrayList<>(productDto.getGalleryImages()));
        }
    }

    private void deleteProductImage(Product product) {
        if (product.getImageUrl() != null) {
            fileStorageService.deleteProductImage(product.getImageUrl());
        }
    }

    private String extractFileNameFromUrl(String imageUrl) {
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }

    // MÉTHODES PRIVÉES - VALIDATION

    private void validateProductId(Long id) {
        if (id == null || id <= 0) {
            throw exceptionFactory.validationError("productId", messageService.getMessage("validation.product.id.invalid"));
        }
    }

    private void validateProductForCreation(ProductDto productDto) {
        if (productDto == null) {
            throw exceptionFactory.validationError("productDto", messageService.getMessage("validation.product.create.required"));
        }
        if (productDto.getName() == null || productDto.getName().trim().isEmpty()) {
            throw exceptionFactory.validationError("name", messageService.getMessage("validation.product.name.required"));
        }
        if (productDto.getPrice() == null || productDto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw exceptionFactory.validationError("price", messageService.getMessage("validation.product.price.invalid"));
        }
        if (productDto.getCategoryId() == null) {
            throw exceptionFactory.validationError("categoryId", messageService.getMessage("validation.product.categoryId.required"));
        }
        if (productDto.getName().length() > 250) {
            throw exceptionFactory.validationError("name", messageService.getMessage("validation.product.name.tooLong", 250));
        }
        if (productDto.getDescription() != null && productDto.getDescription().length() > 500) {
            throw exceptionFactory.validationError("description", messageService.getMessage("validation.product.description.tooLong", 500));
        }
    }

    private void validateProductForUpdate(ProductDto productDto) {
        if (productDto == null) {
            throw exceptionFactory.validationError("productDto", messageService.getMessage("validation.product.update.required"));
        }
        if (productDto.getProductId() == null) {
            throw exceptionFactory.validationError("productId", messageService.getMessage("validation.product.id.requiredForUpdate"));
        }
        validateProductForCreation(productDto);
    }

    private Specification<Product> buildInactiveProductsSpecification() {
        return (root, query, cb) -> cb.isFalse(root.get("isActive"));
    }

    // MAPPER

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

}