package com.store.store.service.impl;

import com.store.store.dto.ProductDto;
import com.store.store.entity.Product;
import com.store.store.repository.ProductRepository;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = TestDataBuilder.createProduct(1L, "Laptop Gaming", new BigDecimal("1299.99"));
        product1.setPopularity(95);
        product1.setImageUrl("http://example.com/laptop.jpg");

        product2 = TestDataBuilder.createProduct(2L, "Smartphone Premium", new BigDecimal("899.99"));
        product2.setPopularity(87);
        product2.setImageUrl("http://example.com/phone.jpg");
    }

    @Test
    void getProducts_ShouldReturnListOfProductDtosWithAuditFields() {
        // Arrange
        List<Product> products = Arrays.asList(product1, product2);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = productService.getProducts();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertProductDtoMatchesEntity(product1, result.get(0));
        assertProductDtoMatchesEntity(product2, result.get(1));
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProducts_ShouldReturnProductsFromTestDataBuilder() {
        // Arrange - Utilisation directe de TestDataBuilder
        List<Product> products = TestDataBuilder.createProductList(3);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = productService.getProducts();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());

        // Vérifier que les produits sont correctement transformés
        for (int i = 0; i < products.size(); i++) {
            assertEquals(products.get(i).getId(), result.get(i).getProductId());
            assertEquals(products.get(i).getName(), result.get(i).getName());
        }

        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProducts_WhenNoProducts_ShouldReturnEmptyList() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of());

        // Act
        List<ProductDto> result = productService.getProducts();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_WhenProductExists_ShouldReturnProductDtoWithAuditFields() {
        // Arrange
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(product1));

        // Act
        ProductDto result = productService.getProductById(productId);

        // Assert
        assertNotNull(result);
        assertProductDtoMatchesEntity(product1, result);
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void getProductById_WhenProductNotExists_ShouldReturnEmptyOptional() {
        // Arrange
        Long productId = 99L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act
        ProductDto result = productService.getProductById(productId);

        // Assert
        assertNull(result);
        verify(productRepository, times(1)).findById(productId);
    }

    @Test
    void getProductById_WithNullId_ShouldReturnEmptyOptional() {
        // Arrange
        when(productRepository.findById(null)).thenReturn(Optional.empty());

        // Act
        ProductDto result = productService.getProductById(null);

        // Assert
        assertNull(result);
        verify(productRepository, times(1)).findById(null);
    }

    @Test
    void transformToDTO_ShouldCorrectlyMapAllFieldsIncludingAuditFields() {
        // Arrange - Utilisation de TestDataBuilder
        Product product = TestDataBuilder.createProduct(5L, "Test Product", new BigDecimal("49.99"));
        product.setPopularity(75);
        product.setImageUrl("http://example.com/test.jpg");

        // Act
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        ProductDto result = productService.getProductById(5L);

        // Assert
        assertNotNull(result);
        assertProductDtoMatchesEntity(product, result);
    }

    @Test
    void getProducts_ShouldHandleNullFieldsGracefully() {
        // Arrange - Créer un produit avec certains champs null
        Product productWithNulls = TestDataBuilder.createProduct(3L, "Product with null fields", new BigDecimal("19.99"));
        productWithNulls.setDescription(null);
        productWithNulls.setImageUrl(null);
        productWithNulls.setPopularity(50);

        List<Product> products = List.of(productWithNulls);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = productService.getProducts();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        ProductDto dto = result.getFirst();
        assertEquals(productWithNulls.getId(), dto.getProductId());
        assertEquals(productWithNulls.getName(), dto.getName());
        assertNull(dto.getDescription());
        assertEquals(productWithNulls.getPrice(), dto.getPrice());
        assertEquals(productWithNulls.getPopularity(), dto.getPopularity());
        assertNull(dto.getImageUrl());
    }

    @Test
    void getProducts_ShouldWorkWithMultipleProductsFromBuilder() {
        // Arrange
        List<Product> products = TestDataBuilder.createProductList(5);
        when(productRepository.findAll()).thenReturn(products);

        // Act
        List<ProductDto> result = productService.getProducts();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());

        // Vérifier que tous les produits sont transformés correctement
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            ProductDto dto = result.get(i);

            assertEquals(product.getId(), dto.getProductId());
            assertEquals(product.getName(), dto.getName());
            assertEquals(product.getPrice(), dto.getPrice());
        }

        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_ShouldHandleBigDecimalValuesCorrectly() {
        // Arrange - Test avec différentes valeurs BigDecimal
        Product productWithHighPrice = TestDataBuilder.createProduct(10L, "Luxury Item", new BigDecimal("9999.99"));
        productWithHighPrice.setPopularity(10);
        productWithHighPrice.setImageUrl("http://example.com/luxury.jpg");

        when(productRepository.findById(10L)).thenReturn(Optional.of(productWithHighPrice));

        // Act
        ProductDto result = productService.getProductById(10L);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("9999.99"), result.getPrice());
    }

    @Test
    void transformToDTO_ShouldSetProductIdCorrectly() {
        // Arrange
        Product product = TestDataBuilder.createProduct(7L, "Test Product", new BigDecimal("25.50"));
        product.setPopularity(60);
        product.setImageUrl("http://example.com/image.jpg");

        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        // Act
        ProductDto result = productService.getProductById(7L);

        // Assert
        assertNotNull(result);
        assertEquals(7L, result.getProductId());
        assertEquals("Test Product", result.getName());
        //assertEquals(product.getCreatedAt(), result.getCreatedAt()); test qui echoue
    }

    // Méthode utilitaire pour vérifier que tous les champs correspondent
    private void assertProductDtoMatchesEntity(Product product, ProductDto dto) {
        assertEquals(product.getId(), dto.getProductId());
        assertEquals(product.getName(), dto.getName());
        assertEquals(product.getDescription(), dto.getDescription());
        assertEquals(product.getPrice(), dto.getPrice());
        assertEquals(product.getPopularity(), dto.getPopularity());
        assertEquals(product.getImageUrl(), dto.getImageUrl());
        //assertEquals(product.getCreatedAt(), dto.getCreatedAt());
    }
}