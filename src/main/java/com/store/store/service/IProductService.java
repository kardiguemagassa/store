package com.store.store.service;

import com.store.store.dto.ProductDto;
import com.store.store.dto.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IProductService {

    // =====================================================
    // MÉTHODES DE RECHERCHE ET LECTURE
    // =====================================================
    Page<ProductDto> searchProducts(ProductSearchCriteria criteria, Pageable pageable);

    List<ProductDto> getAllProducts();
    Page<ProductDto> getAllProducts(Pageable pageable);

    ProductDto getProductById(Long id);
    Page<ProductDto> getActiveProducts(Pageable pageable);

    // =====================================================
    // CRUD PRINCIPAL
    // =====================================================
    ProductDto createProduct(ProductDto productDto);
    ProductDto updateProduct(Long id, ProductDto productDto);
    void deleteProduct(Long id);
    ProductDto restoreProduct(Long id);

    // =====================================================
    // MÉTHODES MÉTIER SPÉCIALISÉES
    // =====================================================
    Page<ProductDto> getFeaturedProducts(Pageable pageable);
    Page<ProductDto> getProductsOnSale(Pageable pageable);

    long countActiveProducts();
    long countOutOfStockProducts();

    // =====================================================
    // GESTION DES IMAGES
    // =====================================================
    String uploadProductImage(Long productId, MultipartFile imageFile) throws IOException;
    void deleteProductImage(Long productId);
    byte[] getProductImageBytes(Long productId);
    List<String> uploadProductImages(Long productId, List<MultipartFile> imageFiles) throws IOException;

    // =====================================================
    // GALERIE D'IMAGES
    // =====================================================
    String addToProductGallery(Long productId, MultipartFile imageFile) throws IOException;
    void removeFromProductGallery(Long productId, String imageUrl);
    void reorderGalleryImages(Long productId, List<String> imageUrlsInOrder);
}