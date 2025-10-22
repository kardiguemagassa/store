package com.store.store.service.impl;

import com.store.store.dto.ProductDto;
import com.store.store.entity.Product;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.exception.ResourceNotFoundException;
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
    public List<ProductDto> getProductsByCategory(String category) {
        try {
            log.info("Fetching products by category: {}", category);
            validateCategory(category);

            List<Product> products = productRepository.findByCategoryIgnoreCase(category);

            log.info("Found {} products in category: {}", products.size(), category);
            return products.stream()
                    .map(this::transformToDTO)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching products by category: {}", category, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.byCategory.failed", category)
            );
        } catch (Exception e) {
            log.error("Unexpected error while fetching products by category: {}", category, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.fetch.byCategory", category)
            );
        }
    }

    @Override
    public Page<ProductDto> getProductsByCategory(String category, Pageable pageable) {
        try {
            log.info("Fetching products by category: {} with pagination: page {}, size {}",
                    category, pageable.getPageNumber(), pageable.getPageSize());

            validateCategory(category);

            Page<Product> productPage = productRepository.findByCategoryIgnoreCase(category, pageable);

            log.info("Found {} products in category: {} on page {}",
                    productPage.getContent().size(), category, pageable.getPageNumber());
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while fetching paginated products by category: {}", category, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.fetch.byCategory.paginated.failed", category)
            );
        } catch (Exception e) {
            log.error("Unexpected error while fetching paginated products by category: {}", category, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.fetch.byCategory.paginated", category)
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

            Product product = transformToEntity(productDto);
            product.setPopularity(0); // Popularité initiale
            Product savedProduct = productRepository.save(product);

            log.info("Product created successfully with ID: {}", savedProduct.getId());
            return transformToDTO(savedProduct);

        } catch (DataAccessException e) {
            log.error("Database error while creating product: {}", productDto.getName(), e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.create.failed")
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while creating product: {}", productDto.getName(), e);
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

            // Mise à jour des champs
            existingProduct.setName(productDto.getName());
            existingProduct.setDescription(productDto.getDescription());
            existingProduct.setPrice(productDto.getPrice());
            existingProduct.setCategory(productDto.getCategory());
            existingProduct.setImageUrl(productDto.getImageUrl());

            Product updatedProduct = productRepository.save(existingProduct);
            log.info("Product updated successfully: {}", id);
            return transformToDTO(updatedProduct);

        } catch (ResourceNotFoundException e) {
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
    public Page<ProductDto> searchProductsByCategory(String category, String query, Pageable pageable) {
        try {
            log.info("Searching products in category: {} with query: {}", category, query);
            validateCategory(category);

            if (query == null || query.trim().isEmpty()) {
                return getProductsByCategory(category, pageable);
            }

            Page<Product> productPage = productRepository.findByCategoryAndNameContainingIgnoreCase(category, query, pageable);
            log.info("Found {} products in category: {} for query: {}", productPage.getContent().size(), category, query);
            return productPage.map(this::transformToDTO);

        } catch (DataAccessException e) {
            log.error("Database error while searching products in category: {} with query: {}", category, query, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.product.search.byCategory.failed", category, query)
            );
        } catch (Exception e) {
            log.error("Unexpected error while searching products in category: {} with query: {}", category, query, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.product.search.byCategory", category, query)
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

    private void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw exceptionFactory.validationError("category",
                    getLocalizedMessage("validation.product.category.required"));
        }

        if (category.length() > 100) {
            throw exceptionFactory.validationError("category",
                    getLocalizedMessage("validation.product.category.tooLong", 100));
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

        if (productDto.getCategory() == null || productDto.getCategory().trim().isEmpty()) {
            throw exceptionFactory.validationError("category",
                    getLocalizedMessage("validation.product.category.required"));
        }

        if (productDto.getName().length() > 250) {
            throw exceptionFactory.validationError("name",
                    getLocalizedMessage("validation.product.name.tooLong", 250));
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

    private ProductDto transformToDTO(Product product) {
        ProductDto productDto = new ProductDto();
        BeanUtils.copyProperties(product, productDto);
        productDto.setProductId(product.getId());
        return productDto;
    }

    private Product transformToEntity(ProductDto productDto) {
        Product product = new Product();
        BeanUtils.copyProperties(productDto, product);
        product.setId(productDto.getProductId());
        return product;
    }

    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}