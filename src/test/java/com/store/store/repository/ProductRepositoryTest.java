package com.store.store.repository;

import com.store.store.entity.Product;
import com.store.store.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour ProductRepository
 * Utilise H2 en mémoire configuré dans application-test.properties
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@DisplayName("Tests du ProductRepository")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Nettoyer la base avant chaque test
        productRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Devrait sauvegarder un produit avec succès")
    void shouldSaveProduct() {
        // Given
        Product product = TestDataBuilder.createProduct(null, "Laptop", new BigDecimal("999.99"));

        // When
        Product savedProduct = productRepository.save(product);

        // Then
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("Laptop");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(savedProduct.getDescription()).isEqualTo("Test description for Laptop");
        assertThat(savedProduct.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Devrait trouver un produit par son ID")
    void shouldFindProductById() {
        // Given
        Product product = TestDataBuilder.createProduct(null, "Smartphone", new BigDecimal("599.99"));
        Product savedProduct = entityManager.persistAndFlush(product);
        Long productId = savedProduct.getId();

        // When
        Optional<Product> foundProduct = productRepository.findById(productId);

        // Then
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Smartphone");
        assertThat(foundProduct.get().getPrice()).isEqualByComparingTo(new BigDecimal("599.99"));
    }

    @Test
    @DisplayName("Devrait retourner Optional.empty() pour un ID inexistant")
    void shouldReturnEmptyForNonExistentId() {
        // When
        Optional<Product> foundProduct = productRepository.findById(999L);

        // Then
        assertThat(foundProduct).isEmpty();
    }

    @Test
    @DisplayName("Devrait récupérer tous les produits")
    void shouldFindAllProducts() {
        // Given
        Product p1 = TestDataBuilder.createProduct(null, "Product 1", new BigDecimal("50.00"));
        Product p2 = TestDataBuilder.createProduct(null, "Product 2", new BigDecimal("100.00"));
        Product p3 = TestDataBuilder.createProduct(null, "Product 3", new BigDecimal("150.00"));
        Product p4 = TestDataBuilder.createProduct(null, "Product 4", new BigDecimal("200.00"));
        Product p5 = TestDataBuilder.createProduct(null, "Product 5", new BigDecimal("250.00"));

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.persist(p4);
        entityManager.persist(p5);
        entityManager.flush();

        // When
        List<Product> foundProducts = productRepository.findAll();

        // Then
        assertThat(foundProducts).hasSize(5);
        assertThat(foundProducts).extracting(Product::getName)
                .containsExactlyInAnyOrder("Product 1", "Product 2", "Product 3", "Product 4", "Product 5");
    }

    @Test
    @DisplayName("Devrait retourner une liste vide quand aucun produit n'existe")
    void shouldReturnEmptyListWhenNoProducts() {
        // When
        List<Product> foundProducts = productRepository.findAll();

        // Then
        assertThat(foundProducts).isEmpty();
    }

    @Test
    @DisplayName("Devrait mettre à jour un produit existant")
    void shouldUpdateExistingProduct() {
        // Given
        Product product = TestDataBuilder.createProduct(null, "Tablet", new BigDecimal("399.99"));
        Product savedProduct = entityManager.persistAndFlush(product);
        Long productId = savedProduct.getId();

        // When
        savedProduct.setName("Tablet Pro");
        savedProduct.setPrice(new BigDecimal("499.99"));
        savedProduct.setPopularity(75);
        Product updatedProduct = productRepository.save(savedProduct);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Product> retrievedProduct = productRepository.findById(productId);
        assertThat(retrievedProduct).isPresent();
        assertThat(retrievedProduct.get().getName()).isEqualTo("Tablet Pro");
        assertThat(retrievedProduct.get().getPrice()).isEqualByComparingTo(new BigDecimal("499.99"));
        assertThat(retrievedProduct.get().getPopularity()).isEqualTo(75);
    }

    @Test
    @DisplayName("Devrait supprimer un produit par son ID")
    void shouldDeleteProductById() {
        // Given
        Product product = TestDataBuilder.createProduct(null, "Headphones", new BigDecimal("79.99"));
        Product savedProduct = entityManager.persistAndFlush(product);
        Long productId = savedProduct.getId();

        // When
        productRepository.deleteById(productId);
        entityManager.flush();

        // Then
        Optional<Product> deletedProduct = productRepository.findById(productId);
        assertThat(deletedProduct).isEmpty();
    }

    @Test
    @DisplayName("Devrait supprimer un produit par entité")
    void shouldDeleteProductByEntity() {
        // Given
        Product product = TestDataBuilder.createProduct(null, "Keyboard", new BigDecimal("49.99"));
        Product savedProduct = entityManager.persistAndFlush(product);
        Long productId = savedProduct.getId();

        // When
        productRepository.delete(savedProduct);
        entityManager.flush();

        // Then
        Optional<Product> deletedProduct = productRepository.findById(productId);
        assertThat(deletedProduct).isEmpty();
    }

    @Test
    @DisplayName("Devrait compter le nombre total de produits")
    void shouldCountAllProducts() {
        // Given
        Product p1 = TestDataBuilder.createProduct(null, "Product 1", new BigDecimal("50.00"));
        Product p2 = TestDataBuilder.createProduct(null, "Product 2", new BigDecimal("100.00"));
        Product p3 = TestDataBuilder.createProduct(null, "Product 3", new BigDecimal("150.00"));
        Product p4 = TestDataBuilder.createProduct(null, "Product 4", new BigDecimal("200.00"));
        Product p5 = TestDataBuilder.createProduct(null, "Product 5", new BigDecimal("250.00"));
        Product p6 = TestDataBuilder.createProduct(null, "Product 6", new BigDecimal("300.00"));
        Product p7 = TestDataBuilder.createProduct(null, "Product 7", new BigDecimal("350.00"));

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.persist(p4);
        entityManager.persist(p5);
        entityManager.persist(p6);
        entityManager.persist(p7);
        entityManager.flush();

        // When
        long count = productRepository.count();

        // Then
        assertThat(count).isEqualTo(7);
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'un produit par ID")
    void shouldCheckProductExistence() {
        // Given
        Product product = TestDataBuilder.createProduct(null, "Mouse", new BigDecimal("29.99"));
        Product savedProduct = entityManager.persistAndFlush(product);
        Long productId = savedProduct.getId();

        // When
        boolean exists = productRepository.existsById(productId);
        boolean notExists = productRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Devrait supprimer tous les produits")
    void shouldDeleteAllProducts() {
        // Given
        Product p1 = TestDataBuilder.createProduct(null, "Product 1", new BigDecimal("50.00"));
        Product p2 = TestDataBuilder.createProduct(null, "Product 2", new BigDecimal("100.00"));
        Product p3 = TestDataBuilder.createProduct(null, "Product 3", new BigDecimal("150.00"));

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.flush();

        // When
        productRepository.deleteAll();
        entityManager.flush();

        // Then
        assertThat(productRepository.count()).isZero();
        assertThat(productRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Devrait persister et récupérer toutes les propriétés d'un produit")
    void shouldPersistAllProductProperties() {
        // Given
        Product product = TestDataBuilder.createProduct(null, "Complete Product", new BigDecimal("199.99"));
        product.setDescription("Description complète du produit");
        product.setImageUrl("https://example.com/product.jpg");
        product.setPopularity(85);

        // When
        Product savedProduct = productRepository.save(product);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Product> retrievedProduct = productRepository.findById(savedProduct.getId());
        assertThat(retrievedProduct).isPresent();
        Product found = retrievedProduct.get();

        assertThat(found.getName()).isEqualTo("Complete Product");
        assertThat(found.getDescription()).isEqualTo("Description complète du produit");
        assertThat(found.getPrice()).isEqualByComparingTo(new BigDecimal("199.99"));
        assertThat(found.getImageUrl()).isEqualTo("https://example.com/product.jpg");
        assertThat(found.getPopularity()).isEqualTo(85);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getCreatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("Devrait gérer les produits avec des prix décimaux précis")
    void shouldHandlePreciseDecimalPrices() {
        // Given
        Product product1 = TestDataBuilder.createProduct(null, "Product A", new BigDecimal("19.99"));
        Product product2 = TestDataBuilder.createProduct(null, "Product B", new BigDecimal("0.01"));
        Product product3 = TestDataBuilder.createProduct(null, "Product C", new BigDecimal("9999.99"));

        // When
        productRepository.saveAll(List.of(product1, product2, product3));
        entityManager.flush();

        // Then
        List<Product> allProducts = productRepository.findAll();
        assertThat(allProducts).hasSize(3);
        assertThat(allProducts).extracting(Product::getPrice)
                .containsExactlyInAnyOrder(
                        new BigDecimal("19.99"),
                        new BigDecimal("0.01"),
                        new BigDecimal("9999.99")
                );
    }
}