package com.store.store.service;

import com.store.store.dto.product.ProductDto;
import com.store.store.dto.product.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
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
    Page<ProductDto> getInactiveProducts(Pageable pageable);

    // CRUD PRINCIPAL
    ProductDto createProduct(ProductDto productDto);
    ProductDto updateProduct(Long id, ProductDto productDto);
    void deleteProduct(Long id);
    ProductDto restoreProduct(Long id);

    // METHODS MÉTIER SPÉCIALISÉES
    Page<ProductDto> getFeaturedProducts(Pageable pageable);
    Page<ProductDto> getProductsOnSale(Pageable pageable);

    long countActiveProducts();
    long countOutOfStockProducts();
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