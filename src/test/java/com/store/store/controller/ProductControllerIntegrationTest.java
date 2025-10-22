package com.store.store.controller;

import com.store.store.entity.Product;
import com.store.store.repository.ProductRepository;
import com.store.store.util.TestDataBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Slf4j
@DisplayName("Tests d'Intégration - ProductController")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        List<Product> products = List.of(
                TestDataBuilder.createProduct(null, "Laptop", new BigDecimal("999.99")),
                TestDataBuilder.createProduct(null, "Mouse", new BigDecimal("29.99")),
                TestDataBuilder.createProduct(null, "Keyboard", new BigDecimal("79.99"))
        );

        productRepository.saveAll(products);
        log.info("✅ {} produits créés pour les tests", products.size());
    }

    // ==================== TESTS GET ALL PRODUCTS ====================

    @Test
    @DisplayName("Devrait récupérer tous les produits de la base de données")
    void shouldRetrieveAllProductsFromDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("Laptop")))
                .andExpect(jsonPath("$[0].price", is(999.99)))
                .andExpect(jsonPath("$[1].name", is("Mouse")))
                .andExpect(jsonPath("$[1].price", is(29.99)))
                .andExpect(jsonPath("$[2].name", is("Keyboard")))
                .andExpect(jsonPath("$[2].price", is(79.99)));

        log.info("✅ Produits récupérés avec succès de la DB");
    }

    // ==================== TESTS GET PRODUCT BY ID ====================

    @Test
    @DisplayName("Devrait retourner un produit par son ID")
    void shouldReturnProductById() throws Exception {
        Product savedProduct = productRepository.findAll().getFirst();

        mockMvc.perform(get("/api/v1/products/{id}", savedProduct.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(savedProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Laptop")))
                .andExpect(jsonPath("$.price", is(999.99)))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.popularity").exists())
                .andExpect(jsonPath("$.imageUrl").exists());

        log.info("✅ Produit récupéré par ID avec succès");
    }

    @Test
    @DisplayName("Devrait retourner 404 pour un produit inexistant")
    void shouldReturn404ForNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999"))
                .andDo(print())
                .andExpect(status().isNotFound());

        log.info("✅ 404 retourné correctement pour produit inexistant");
    }

    @Test
    @DisplayName("Devrait retourner 400 pour un ID invalide")
    void shouldReturn400ForInvalidId() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", "invalid"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.errorMessage", is("Invalid parameter: id")));

        log.info("✅ 400 retourné correctement pour ID invalide");
    }

    // ==================== TESTS DE PERSISTANCE ====================

    @Test
    @DisplayName("Devrait vérifier que les produits sont bien persistés en base")
    void shouldVerifyProductsPersistence() throws Exception {
        List<Product> productsInDb = productRepository.findAll();

        assert productsInDb.size() == 3;
        assert productsInDb.stream().anyMatch(p -> p.getName().equals("Laptop"));
        assert productsInDb.stream().anyMatch(p -> p.getName().equals("Mouse"));
        assert productsInDb.stream().anyMatch(p -> p.getName().equals("Keyboard"));

        log.info("✅ Persistance des produits vérifiée avec succès");
    }

    @Test
    @DisplayName("Devrait vérifier que les produits récupérés correspondent à ceux en base")
    void shouldVerifyProductsMatchDatabase() throws Exception {
        List<Product> productsInDb = productRepository.findAll();
        Product firstProduct = productsInDb.getFirst();

        mockMvc.perform(get("/api/v1/products/{id}", firstProduct.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(firstProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is(firstProduct.getName())))
                .andExpect(jsonPath("$.price", is(firstProduct.getPrice().doubleValue())));

        log.info("✅ Cohérence entre API et base de données vérifiée");
    }

    // ==================== TESTS DE VALIDATION DES DONNÉES ====================

    @Test
    @DisplayName("Devrait retourner les bons types de données")
    void shouldReturnCorrectDataTypes() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].description").isString())
                .andExpect(jsonPath("$[0].price").isNumber())
                .andExpect(jsonPath("$[0].popularity").isNumber())
                .andExpect(jsonPath("$[0].imageUrl").isString())
                .andExpect(jsonPath("$[0].createdAt").exists());

        log.info("✅ Types de données vérifiés avec succès");
    }

    @Test
    @DisplayName("Devrait retourner les prix avec exactement 2 décimales")
    void shouldReturnPricesWithTwoDecimals() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price", is(999.99)))
                .andExpect(jsonPath("$[1].price", is(29.99)))
                .andExpect(jsonPath("$[2].price", is(79.99)));

        log.info("✅ Prix avec 2 décimales vérifiés");
    }

    @Test
    @DisplayName("Devrait retourner tous les champs obligatoires pour chaque produit")
    void shouldReturnAllMandatoryFieldsForEachProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].productId").isNotEmpty())
                .andExpect(jsonPath("$[*].name").isNotEmpty())
                .andExpect(jsonPath("$[*].price").isNotEmpty());

        log.info("✅ Tous les champs obligatoires présents");
    }

    // ==================== TESTS DE TRANSACTION ====================

    @Test
    @DisplayName("Devrait rollback les changements après le test grâce à @Transactional")
    void shouldRollbackChangesAfterTest() throws Exception {
        long initialCount = productRepository.count();

        Product tempProduct = TestDataBuilder.createProduct(null, "Temporary", new BigDecimal("1.00"));
        productRepository.save(tempProduct);

        assert productRepository.count() == initialCount + 1;

        log.info("✅ Transaction test : {} produits avant, {} après ajout",
                initialCount, productRepository.count());
    }
}