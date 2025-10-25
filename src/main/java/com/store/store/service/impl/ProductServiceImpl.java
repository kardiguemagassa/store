package com.store.store.service.impl;

import com.store.store.dto.ProductDto;
import com.store.store.entity.Category;
import com.store.store.entity.Product;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.repository.CategoryRepository;
import com.store.store.repository.ProductRepository;
import com.store.store.service.IProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;
    private final CategoryRepository categoryRepository;

    // LECTURE
    @Cacheable("products")
    @Override
    public List<ProductDto> getProducts() {
        try {
            log.info("Fetching all products from database");
            List<Product> products = productRepository.findAll();
            log.info("Found {} products", products.size());
            return products.stream()
                    .map(this::transformToDTO)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("Database error while fetching products", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.failed")
            );
        } catch (Exception e) {
            log.error("Unexpected error while fetching products", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.fetch")
            );
        }
    }

    @Override
    public Page<ProductDto> getProducts(Pageable pageable) {
        try {
            log.info("Fetching products with pagination: page {}, size {}",
                    pageable.getPageNumber(), pageable.getPageSize());

            Page<Product> productPage = productRepository.findAll(pageable);

            log.info("Found {} products on page {}", productPage.getContent().size(), pageable.getPageNumber());
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while fetching paginated products", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.paginated.failed")
            );
        } catch (Exception e) {
            log.error("Unexpected error while fetching paginated products", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.fetch.paginated")
            );
        }
    }

    @Override
    public ProductDto getProductById(Long id) {
        try {
            log.info("Fetching product by ID: {}", id);
            validateProductId(id);

            Product product = productRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.productNotFound(id));

            log.info("Product found: {} (ID: {})", product.getName(), id);
            return transformToDTO(product);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.byId.failed")
            );
        } catch (Exception e) {
            log.error("Unexpected error while fetching product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.fetch.byId")
            );
        }
    }

    @Override
    public List<ProductDto> getProductsByCategoryCode(String categoryCode) {
        try {
            log.info("Fetching products by category code: {}", categoryCode);
            validateCategoryCode(categoryCode);

            // ✅ NOUVEAU : findByCategoryCode(categoryCode)
            List<Product> products = productRepository.findByCategoryCode(categoryCode);

            log.info("Found {} products in category: {}", products.size(), categoryCode);
            return products.stream()
                    .map(this::transformToDTO)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching products by category: {}", categoryCode, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.byCategoryCode.failed", categoryCode)
            );
        } catch (Exception e) {
            log.error("Unexpected error while fetching products by category: {}", categoryCode, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.fetch.byCategoryCode", categoryCode)
            );
        }
    }

    @Override
    public Page<ProductDto> getProductsByCategoryCode(String categoryCode, Pageable pageable) {
        try {
            log.info("Fetching products by category: {} with pagination: page {}, size {}",
                    categoryCode, pageable.getPageNumber(), pageable.getPageSize());

            validateCategoryCode(categoryCode);

            // ✅ NOUVEAU : findByCategoryCode avec Pageable
            Page<Product> productPage = productRepository.findByCategoryCode(categoryCode, pageable);

            log.info("Found {} products in category: {} on page {}",
                    productPage.getContent().size(), categoryCode, pageable.getPageNumber());
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while fetching paginated products by category: {}", categoryCode, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.byCategoryCode.paginated.failed", categoryCode)
            );
        } catch (Exception e) {
            log.error("Unexpected error while fetching paginated products by category: {}", categoryCode, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.fetch.byCategoryCode.paginated", categoryCode)
            );
        }
    }

    //CRUD
    @Override
    @Transactional
    public ProductDto createProduct(ProductDto productDto) {
        try {
            log.info("Creating new product: {}", productDto.getName());

            validateProductForCreation(productDto);

            // Vérifier si le produit existe déjà
            if (productRepository.existsByNameIgnoreCase(productDto.getName())) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.product.already.exists", productDto.getName())
                );
            }

            // ✅ NOUVEAU : Récupérer la catégorie
            Category category = categoryRepository.findById(productDto.getCategoryId())
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "id", productDto.getCategoryId().toString()
                    ));

            // Créer le produit
            Product product = new Product();
            product.setName(productDto.getName());
            product.setDescription(productDto.getDescription());
            product.setPrice(productDto.getPrice());
            product.setPopularity(0); // Popularité initiale
            product.setImageUrl(productDto.getImageUrl());
            product.setCategory(category);  // ✅ Définir la relation

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
        } catch (Exception e) {
            log.error("Unexpected error while creating product", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.create")
            );
        }
    }

    @Override
    @Transactional
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        try {
            log.info("Updating product ID: {}", id);
            validateProductId(id);
            validateProductForUpdate(productDto);

            Product existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.productNotFound(id));

            // Vérifier conflit de nom (sauf pour le produit actuel)
            if (!existingProduct.getName().equalsIgnoreCase(productDto.getName()) &&
                    productRepository.existsByNameIgnoreCase(productDto.getName())) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.product.already.exists", productDto.getName())
                );
            }

            // ✅ NOUVEAU : Récupérer la catégorie si categoryId a changé
            if (productDto.getCategoryId() != null &&
                    !productDto.getCategoryId().equals(existingProduct.getCategory().getCategoryId())) {

                Category newCategory = categoryRepository.findById(productDto.getCategoryId())
                        .orElseThrow(() -> exceptionFactory.resourceNotFound(
                                "Category", "id", productDto.getCategoryId().toString()
                        ));

                existingProduct.setCategory(newCategory);
                log.info("Category updated from {} to {}",
                        existingProduct.getCategory().getName(),
                        newCategory.getName());
            }

            // Mise à jour des autres champs
            existingProduct.setName(productDto.getName());
            existingProduct.setDescription(productDto.getDescription());
            existingProduct.setPrice(productDto.getPrice());
            existingProduct.setImageUrl(productDto.getImageUrl());

            // Note : popularity n'est généralement pas modifiable par l'utilisateur
            // Si vous voulez le permettre, ajoutez :
            // if (productDto.getPopularity() != null) {
            //     existingProduct.setPopularity(productDto.getPopularity());
            // }

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
        } catch (Exception e) {
            log.error("Unexpected error while updating product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.update")
            );
        }
    }


    @Override
    @Transactional
    public void deleteProduct(Long id) {
        try {
            log.info("Deleting product ID: {}", id);
            validateProductId(id);

            if (!productRepository.existsById(id)) {
                throw exceptionFactory.productNotFound(id);
            }

            productRepository.deleteById(id);
            log.info("Product deleted successfully: {}", id);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while deleting product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.delete.failed")
            );
        } catch (Exception e) {
            log.error("Unexpected error while deleting product ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.delete")
            );
        }
    }

    //UPLOAD IMAGE
    @Override
    @Transactional
    public String uploadProductImage(Long productId, MultipartFile imageFile) {
        try {
            log.info("Uploading image for product ID: {}", productId);
            validateProductId(productId);
            validateImageFile(imageFile);

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> exceptionFactory.productNotFound(productId));

            // Générer un nom de fichier unique
            String fileName = generateUniqueFileName(imageFile.getOriginalFilename());

            // Sauvegarder le fichier
            String imageUrl = saveImageFile(imageFile, fileName);

            // Mettre à jour le produit
            product.setImageUrl(imageUrl);
            productRepository.save(product);

            log.info("Image uploaded successfully for product ID: {}", productId);
            return imageUrl;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while uploading image for product ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.image.upload.failed")
            );
        } catch (IOException e) {
            log.error("File system error while uploading image for product ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.image.save.failed")
            );
        } catch (Exception e) {
            log.error("Unexpected error while uploading image for product ID: {}", productId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.image.upload")
            );
        }
    }

    //RECHERCHE
    @Override
    public Page<ProductDto> searchProducts(String query, Pageable pageable) {
        try {
            log.info("Searching products with query: {}", query);
            if (query == null || query.trim().isEmpty()) {
                return getProducts(pageable);
            }

            Page<Product> productPage = productRepository.findByNameContainingIgnoreCase(query, pageable);
            log.info("Found {} products for query: {}", productPage.getContent().size(), query);
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while searching products with query: {}", query, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.search.failed", query)
            );
        } catch (Exception e) {
            log.error("Unexpected error while searching products with query: {}", query, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.search", query)
            );
        }
    }

    @Override
    public Page<ProductDto> searchProductsByCategoryCode(String categoryCode, String query, Pageable pageable) {
        try {
            log.info("Searching products in category: {} with query: {}", categoryCode, query);
            validateCategoryCode(categoryCode);

            if (query == null || query.trim().isEmpty()) {
                return getProductsByCategoryCode(categoryCode, pageable);
            }

            // ✅ NOUVEAU : Utiliser la nouvelle méthode du repository
            Page<Product> productPage = productRepository.findByCategoryCodeAndNameContaining(
                    categoryCode, query, pageable
            );

            log.info("Found {} products in category: {} for query: {}",
                    productPage.getContent().size(), categoryCode, query);
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while searching products in category: {} with query: {}",
                    categoryCode, query, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.search.byCategoryCode.failed", categoryCode, query)
            );
        } catch (Exception e) {
            log.error("Unexpected error while searching products in category: {} with query: {}",
                    categoryCode, query, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.search.byCategoryCode", categoryCode, query)
            );
        }
    }

    // ✅ AJOUTER une nouvelle méthode pour créer un produit avec CategoryId
    @Override
    @Transactional
    public ProductDto createProductWithCategory(ProductDto productDto, Long categoryId) {
        try {
            log.info("Creating new product: {} with category ID: {}", productDto.getName(), categoryId);

            validateProductForCreation(productDto);

            // Vérifier que la catégorie existe
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "id", categoryId.toString()
                    ));

            // Vérifier si le produit existe déjà
            if (productRepository.existsByNameIgnoreCase(productDto.getName())) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.product.already.exists", productDto.getName())
                );
            }

            Product product = new Product();
            product.setName(productDto.getName());
            product.setDescription(productDto.getDescription());
            product.setPrice(productDto.getPrice());
            product.setPopularity(0);
            product.setImageUrl(productDto.getImageUrl());
            product.setCategory(category);  // ✅ Définir la relation

            Product savedProduct = productRepository.save(product);

            log.info("Product created successfully with ID: {}", savedProduct.getId());
            return transformToDTO(savedProduct);

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while creating product with category", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.create.failed")
            );
        } catch (Exception e) {
            log.error("Unexpected error while creating product", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.create")
            );
        }
    }

    //VALIDATION
    private void validateProductId(Long id) {
        if (id == null || id <= 0) {
            throw exceptionFactory.validationError("productId",
                    getLocalizedMessage("validation.product.id.invalid"));
        }
    }

    private void validateCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw exceptionFactory.validationError("categoryCode",
                    getLocalizedMessage("validation.product.categoryCode.required"));
        }

        if (categoryCode.length() > 50) {
            throw exceptionFactory.validationError("categoryCode",
                    getLocalizedMessage("validation.product.categoryCode.tooLong", 50));
        }
    }

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

        // ✅ CORRIGER : Valider categoryId au lieu de category
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

    private void validateProductForUpdate(ProductDto productDto) {
        if (productDto == null) {
            throw exceptionFactory.validationError("productDto",
                    getLocalizedMessage("validation.product.update.required"));
        }

        validateProductForCreation(productDto);
    }

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw exceptionFactory.validationError("imageFile",
                    getLocalizedMessage("validation.product.image.required"));
        }

        if (!isValidImageType(imageFile.getContentType())) {
            throw exceptionFactory.validationError("imageFile",
                    getLocalizedMessage("validation.product.image.type.invalid"));
        }

        if (imageFile.getSize() > 5 * 1024 * 1024) {
            throw exceptionFactory.validationError("imageFile",
                    getLocalizedMessage("validation.product.image.size.tooLarge", 5));
        }
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/webp") ||
                        contentType.equals("image/gif")
        );
    }

    // MÉTHODES UTILITAIRES
    private String generateUniqueFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileExtension = originalFileName != null ?
                originalFileName.substring(originalFileName.lastIndexOf(".")) : ".jpg";
        return "product_" + timestamp + fileExtension;
    }

    private String saveImageFile(MultipartFile imageFile, String fileName) throws IOException {
        Path uploadPath = Paths.get("uploads/products");
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/products/" + fileName;
    }

    /**
     * ✅ Transforme une Entity Product en ProductDto
     */
    private ProductDto transformToDTO(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setProductId(product.getId());
        productDto.setName(product.getName());
        productDto.setDescription(product.getDescription());
        productDto.setPrice(product.getPrice());
        productDto.setPopularity(product.getPopularity());
        productDto.setImageUrl(product.getImageUrl());

        // ✅ AJOUTER les informations de catégorie
        if (product.getCategory() != null) {
            productDto.setCategoryId(product.getCategory().getCategoryId());
            productDto.setCategoryCode(product.getCategory().getCode());
            productDto.setCategoryName(product.getCategory().getName());
            productDto.setCategoryIcon(product.getCategory().getIcon());
        }

        return productDto;
    }

    /**
     * ✅ Transforme un ProductDto en Entity Product (SANS la relation Category)
     * Note : La catégorie doit être définie séparément via setCateogry()
     */
    private Product transformToEntity(ProductDto productDto) {
        Product product = new Product();
        product.setId(productDto.getProductId());
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setPopularity(productDto.getPopularity());
        product.setImageUrl(productDto.getImageUrl());

        // ⚠️ NE PAS définir la catégorie ici
        // Elle doit être récupérée de la DB et définie explicitement

        return product;
    }

    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}