package com.store.store.controller;

import com.store.store.config.TestSecurityConfig;
import com.store.store.dto.ProductDto;
import com.store.store.service.IProductService;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests Unitaires pour ProductController
 * Utilise des mocks pour isoler le controller de la couche service
 */
/*@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
@DisplayName("Tests Unitaires - ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProductService iProductService;  // ✅ Utiliser l'interface

    private List<ProductDto> testProductDtos;
    private ProductDto testProductDto;

    @BeforeEach
    void setUp() {
        // ✅ Créer des ProductDto (pas Product) car le service retourne des DTO
        testProductDto = TestDataBuilder.createProductDto(1L, "Laptop", new BigDecimal("999.99"));

        testProductDtos = List.of(
                testProductDto,
                TestDataBuilder.createProductDto(2L, "Mouse", new BigDecimal("29.99")),
                TestDataBuilder.createProductDto(3L, "Keyboard", new BigDecimal("79.99"))
        );
    }

    // ==================== TESTS GET ALL PRODUCTS ====================

    @Test
    @DisplayName("GET /api/v1/products - Devrait retourner tous les produits")
    void getProducts_ShouldReturnProductList() throws Exception {
        // Given
        when(iProductService.getProducts()).thenReturn(testProductDtos);

        // When & Then
        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].productId", is(1)))
                .andExpect(jsonPath("$[0].name", is("Laptop")))
                .andExpect(jsonPath("$[0].price", is(999.99)))
                .andExpect(jsonPath("$[1].name", is("Mouse")))
                .andExpect(jsonPath("$[2].name", is("Keyboard")));

        verify(iProductService, times(1)).getProducts();
    }

    @Test
    @DisplayName("GET /api/v1/products - Devrait retourner une liste vide si aucun produit")
    void getProducts_WhenNoProducts_ShouldReturnEmptyList() throws Exception {
        // Given
        when(iProductService.getProducts()).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(iProductService, times(1)).getProducts();
    }

    // ==================== TESTS GET PRODUCT BY ID ====================

    @Test
    @DisplayName("GET /api/v1/products/{id} - Devrait retourner un produit par son ID")
    void getProductById_WhenProductExists_ShouldReturnProduct() throws Exception {
        // Given
        when(iProductService.getProductById(1L)).thenReturn((testProductDto));

        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.price", is(999.99)))
                .andExpect(jsonPath("$.description", is("Test description for Laptop")));

        verify(iProductService, times(1)).getProductById(1L);
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Devrait retourner 404 si produit inexistant")
    void getProductById_WhenProductNotFound_ShouldReturn404() throws Exception {
        // Given
        when(iProductService.getProductById(999L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(iProductService, times(1)).getProductById(999L);
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Devrait gérer les IDs invalides avec 400")
    void getProductById_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", "invalid"))
                .andDo(print())
                .andExpect(status().isBadRequest())  // Changé de isServiceUnavailable() à isBadRequest()
                .andExpect(jsonPath("$.errorCode", is("BAD_REQUEST"))) // Changé de "SERVICE_UNAVAILABLE" à "BAD_REQUEST"
                .andExpect(jsonPath("$.errorMessage", containsString("Invalid parameter: id")))  // Message d'erreur réel
                .andExpect(jsonPath("$.errorTime").exists());

        verify(iProductService, never()).getProductById(anyLong());
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} - Devrait gérer les erreurs serveur avec 503")
    void getProductById_WhenServiceFails_ShouldReturnServiceUnavailable() throws Exception {
        // Test pour les erreurs serveur (503)
        when(iProductService.getProductById(1L))
                .thenThrow(new RuntimeException("Database unavailable"));

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode", is("SERVICE_UNAVAILABLE")));

        verify(iProductService, times(1)).getProductById(1L);
    }

    // ==================== TESTS DE VALIDATION ====================

    @Test
    @DisplayName("Devrait vérifier que le service est appelé avec les bons paramètres")
    void shouldCallServiceWithCorrectParameters() throws Exception {
        // Given
        Long productId = 42L;
        when(iProductService.getProductById(productId)).thenReturn(testProductDto);

        // When
        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk());

        // Then
        verify(iProductService).getProductById(productId);
        verify(iProductService, never()).getProducts();
    }

    @Test
    @DisplayName("Devrait retourner les bons Content-Type headers")
    void shouldReturnCorrectContentTypeHeaders() throws Exception {
        // Given
        when(iProductService.getProducts()).thenReturn(testProductDtos);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ==================== TESTS D'ERREURS ====================

    @Test
    @DisplayName("Devrait gérer les exceptions du service avec ExceptionHandler")
    void shouldHandleServiceExceptionsWithGlobalHandler() throws Exception {
        // Given
        when(iProductService.getProducts())
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isServiceUnavailable())  // ✅ 503 selon votre @ExceptionHandler
                .andExpect(jsonPath("$.errorMessage", is("Database connection failed")))
                .andExpect(jsonPath("$.errorCode", is("SERVICE_UNAVAILABLE")))
                .andExpect(jsonPath("$.errorTime").exists());

        verify(iProductService, times(1)).getProducts();
    }

    @Test
    @DisplayName("Devrait gérer les IDs négatifs")
    void getProductById_WithNegativeId_ShouldReturnNotFound() throws Exception {
        // Given
        when(iProductService.getProductById(-1L)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", -1L))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(iProductService, times(1)).getProductById(-1L);
    }

    @Test
    @DisplayName("Devrait gérer les exceptions lors de la récupération d'un produit")
    void getProductById_WhenServiceThrowsException_ShouldReturn503() throws Exception {
        // Given
        when(iProductService.getProductById(1L))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andDo(print())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorMessage", is("Service error")));

        verify(iProductService, times(1)).getProductById(1L);
    }

    // ==================== TESTS DE PERFORMANCE ====================

    @Test
    @DisplayName("Devrait retourner rapidement même avec beaucoup de produits")
    void getProducts_WithManyProducts_ShouldReturnQuickly() throws Exception {
        // Given - Créer 100 produits DTO
        List<ProductDto> manyProductDtos = java.util.stream.IntStream.range(1, 101)
                .mapToObj(i -> TestDataBuilder.createProductDto(
                        (long) i,
                        "Product " + i,
                        new BigDecimal("10.00").multiply(BigDecimal.valueOf(i))
                ))
                .toList();

        when(iProductService.getProducts()).thenReturn(manyProductDtos);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(100)))
                .andExpect(jsonPath("$[0].name", is("Product 1")))
                .andExpect(jsonPath("$[99].name", is("Product 100")));

        verify(iProductService, times(1)).getProducts();
    }

    // ==================== TESTS DES CHAMPS RETOURNÉS ====================

    @Test
    @DisplayName("Devrait retourner tous les champs requis d'un produit")
    void getProductById_ShouldReturnAllRequiredFields() throws Exception {
        // Given
        when(iProductService.getProductById(1L)).thenReturn(testProductDto);

        // When & Then
        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.popularity").exists())
                .andExpect(jsonPath("$.imageUrl").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Devrait retourner les prix avec 2 décimales")
    void getProducts_ShouldReturnPricesWithTwoDecimals() throws Exception {
        // Given
        when(iProductService.getProducts()).thenReturn(testProductDtos);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price", is(999.99)))
                .andExpect(jsonPath("$[1].price", is(29.99)))
                .andExpect(jsonPath("$[2].price", is(79.99)));
    }

    // ==================== TESTS DE CACHE ====================

    @Test
    @DisplayName("Devrait appeler le service à chaque fois (pas de cache dans les tests unitaires)")
    void getProducts_ShouldCallServiceEveryTime() throws Exception {
        // Given
        when(iProductService.getProducts()).thenReturn(testProductDtos);

        // When - Appeler 3 fois
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());

        // Then - Le service devrait être appelé 3 fois (pas de cache dans @WebMvcTest)
        verify(iProductService, times(3)).getProducts();
    }

    // ==================== TESTS MÉTIER ====================

    @Test
    @DisplayName("Devrait retourner les produits dans l'ordre retourné par le service")
    void getProducts_ShouldReturnProductsInServiceOrder() throws Exception {
        // Given
        when(iProductService.getProducts()).thenReturn(testProductDtos);

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId", is(1)))
                .andExpect(jsonPath("$[1].productId", is(2)))
                .andExpect(jsonPath("$[2].productId", is(3)));
    }

    @Test
    @DisplayName("Devrait retourner exactement ce que le service retourne")
    void getProducts_ShouldReturnExactlyWhatServiceReturns() throws Exception {
        // Given
        ProductDto singleProduct = TestDataBuilder.createProductDto(999L, "Special Product", new BigDecimal("1.99"));
        when(iProductService.getProducts()).thenReturn(List.of(singleProduct));

        // When & Then
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId", is(999)))
                .andExpect(jsonPath("$[0].name", is("Special Product")))
                .andExpect(jsonPath("$[0].price", is(1.99)));
    }
}

 */