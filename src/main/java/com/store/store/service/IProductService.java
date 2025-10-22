package com.store.store.service;

import com.store.store.dto.ProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IProductService {

    List<ProductDto> getProducts();
    ProductDto getProductById(Long id);
    List<ProductDto> getProductsByCategory(String category);

    //PAGINATION
    Page<ProductDto> getProducts(Pageable pageable);
    Page<ProductDto> getProductsByCategory(String category, Pageable pageable);

    //CRUD
    ProductDto createProduct(ProductDto productDto);
    ProductDto updateProduct(Long id, ProductDto productDto);
    void deleteProduct(Long id);

    //UPLOAD IMAGE
    String uploadProductImage(Long productId, MultipartFile imageFile);

    // RECHERCHE AVANCÉE
    Page<ProductDto> searchProducts(String query, Pageable pageable);
    Page<ProductDto> searchProductsByCategory(String category, String query, Pageable pageable);
}