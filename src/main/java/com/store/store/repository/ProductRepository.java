package com.store.store.repository;

import com.store.store.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // ========================================================================
    // MÉTHODES DE BASE AVEC CATÉGORIE
    // ========================================================================

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p")
    List<Product> findAllWithCategory();

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);

    // ========================================================================
    // MÉTHODES SPÉCIFIQUES UTILES (réduites au minimum)
    // ========================================================================

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.isActive = true")
    Page<Product> findActiveProducts(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.category.code = :categoryCode AND p.isActive = true")
    Page<Product> findActiveProductsByCategory(@Param("categoryCode") String categoryCode, Pageable pageable);

    boolean existsByNameIgnoreCase(String name);

    // ========================================================================
    // COMPTAGE POUR STATISTIQUES
    // ========================================================================

    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    long countActiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity = 0")
    long countOutOfStockProducts();

    // ========================================================================
    // RECHERCHE UNIVERSELLE (remplace toutes les autres méthodes de recherche)
    // ========================================================================

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT p FROM Product p WHERE
        (:activeOnly = false OR p.isActive = true) AND
        (:categoryCode IS NULL OR p.category.code = :categoryCode) AND
        (:minPrice IS NULL OR p.price >= :minPrice) AND
        (:maxPrice IS NULL OR p.price <= :maxPrice) AND
        (:searchQuery IS NULL OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
            LOWER(p.description) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
    """)
    Page<Product> findByCriteria(
            @Param("searchQuery") String searchQuery,
            @Param("categoryCode") String categoryCode,
            @Param("activeOnly") boolean activeOnly,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}