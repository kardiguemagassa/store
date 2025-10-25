package com.store.store.service;

import com.store.store.dto.ProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IProductService {

    // LECTURE
    List<ProductDto> getProducts();
    ProductDto getProductById(Long id);

    List<ProductDto> getProductsByCategoryCode(String categoryCode);
    Page<ProductDto> getProductsByCategoryCode(String categoryCode, Pageable pageable);

    // PAGINATION
    Page<ProductDto> getProducts(Pageable pageable);

    // CRUD
    ProductDto createProduct(ProductDto productDto);
    ProductDto createProductWithCategory(ProductDto productDto, Long categoryId);
    ProductDto updateProduct(Long id, ProductDto productDto);
    void deleteProduct(Long id);

    // UPLOAD IMAGE
    String uploadProductImage(Long productId, MultipartFile imageFile);

    // RECHERCHE AVANCÉE
    Page<ProductDto> searchProducts(String query, Pageable pageable);
    Page<ProductDto> searchProductsByCategoryCode(String categoryCode, String query, Pageable pageable);
}