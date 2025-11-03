package com.store.store.service;

import com.store.store.dto.ProductDto;
import com.store.store.dto.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Interface defining operations related to product management in the system.
 *
 * Provides methods for searching, creating, updating, deleting, and restoring products.
 * Includes specialized business logic methods, image management, and utility functionalities
 * to support the handling of product information and associated resources.
 *
 *  @author Kardigué
 *  * @version 3.0 - Production Ready
 *  * @since 2025-10-27
 */
public interface IProductService {

    // METHODS DE RECHERCHE ET LECTURE
    Page<ProductDto> searchProducts(ProductSearchCriteria criteria, Pageable pageable);

    List<ProductDto> getAllProducts();
    Page<ProductDto> getAllProducts(Pageable pageable);

    ProductDto getProductById(Long id);
    Page<ProductDto> getActiveProducts(Pageable pageable);

    /**
     * Retrieves a paginated list of inactive products.
     *
     * An inactive product is identified by its `isActive` status set to false.
     *
     * @param pageable Pagination information including page number, size, and sort options
     * @return A paginated list containing inactive products, represented as `ProductDto` objects
     */
    Page<ProductDto> getInactiveProducts(Pageable pageable);

    // CRUD PRINCIPAL
    ProductDto createProduct(ProductDto productDto);
    ProductDto updateProduct(Long id, ProductDto productDto);
    void deleteProduct(Long id);
    ProductDto restoreProduct(Long id);

    // METHODS MÉTIER SPÉCIALISÉES

    /**
     * Retrieves a paginated list of featured products.
     *
     * Featured products are items highlighted based on predefined business criteria,
     * such as popularity, recent additions, or promotional status.
     *
     * @param pageable The pagination information, including page number, size, and sorting options
     * @return A paginated collection of featured products, represented as `ProductDto` objects
     */
    Page<ProductDto> getFeaturedProducts(Pageable pageable);

    /**
     * Retrieves a paginated list of products currently on sale.
     *
     * Products on sale are identified based on specific business criteria,
     * such as discounts, promotional prices, or special offers.
     *
     * @param pageable The pagination information, including page number, size, and sorting options
     * @return A paginated collection of products on sale, represented as `ProductDto` objects
     */
    Page<ProductDto> getProductsOnSale(Pageable pageable);

    /**
     * Counts the total number of active products in the system.
     *
     * An active product is identified by its `isActive` status set to true.
     *
     * @return The total count of active products as a long value.
     */
    long countActiveProducts();

    /**
     * Counts the total number of out-of-stock products.
     *
     * This method calculates the number of products that are unavailable for sale due to
     * insufficient stock quantities. A product is considered out-of-stock when its stock
     * level reaches zero or falls below a predefined threshold.
     *
     * @return The total count of out-of-stock products as a long value.
     */
    long countOutOfStockProducts();

    /**
     * Counts the total number of inactive products in the system.
     *
     * An inactive product is identified by its `isActive` status set to false.
     *
     * @return The total number of inactive products as a long value.
     */
    long countInactiveProducts();


    // GESTION DES IMAGES
    String uploadProductImage(Long productId, MultipartFile imageFile) throws IOException;
    void deleteProductImage(Long productId);
    byte[] getProductImageBytes(Long productId);
    List<String> uploadProductImages(Long productId, List<MultipartFile> imageFiles) throws IOException;


    // GALERIE D'IMAGES
    String addToProductGallery(Long productId, MultipartFile imageFile) throws IOException;
    void removeFromProductGallery(Long productId, String imageUrl);
    void reorderGalleryImages(Long productId, List<String> imageUrlsInOrder);
}