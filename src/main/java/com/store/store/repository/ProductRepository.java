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

    // METHODS DE BASE AVEC CATÉGORIE
    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p")
    List<Product> findAllWithCategory();

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);


    // METHODS SPECIFIQUES UTILES (réduites au minimum)

    /**
     * Retrieves a paginated list of active products. A product is considered active if its `isActive` field is true.
     * The query is executed with an entity graph that includes the "category" attribute to fetch associated categories.
     *
     * @param pageable the pagination information, including page number, size, and sorting details
     * @return a Page containing active products with their associated categories
     */
    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.isActive = true")
    Page<Product> findActiveProducts(Pageable pageable);

    /**
     * Retrieves a paginated list of active products by a specific category.
     * A product is considered active if its `isActive` field is true.
     * The query is executed with an entity graph that includes the "category" attribute
     * to fetch associated categories.
     *
     * @param categoryCode the code of the category to filter products by
     * @param pageable the pagination information, including page number, size, and sorting details
     * @return a Page containing active products associated with the specified category
     */
    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.category.code = :categoryCode AND p.isActive = true")
    Page<Product> findActiveProductsByCategory(@Param("categoryCode") String categoryCode, Pageable pageable);

    /**
     * Checks if an entity exists with the specified name, ignoring case.
     *
     * @param name the name of the entity to check for existence
     * @return true if an entity with the specified name exists, ignoring case; false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    // COMPTAGE POUR STATISTIQUES
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
    long countActiveProducts();

    /**
     * Counts the number of inactive products in the database.
     * A product is considered inactive if its `isActive` field is false.
     *
     * @return the count of inactive products
     */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = false")
    long countInactiveProducts();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity = 0")
    long countOutOfStockProducts();


    // RECHERCHE UNIVERSELLE (remplace toutes les autres méthodes de recherche)

    /**
     * Retrieves a paginated list of products based on the specified search criteria.
     * The query filters products by their active status, category, price range, and/or a search query
     * that matches product names or descriptions. The query also includes an entity graph to fetch
     * the associated "category" attribute.
     *
     * @param searchQuery a text to search for in the product name or description; may be null.
     * @param categoryCode the category code to filter products; may be null.
     * @param activeOnly a flag indicating if only active products should be included; true for active products only, false otherwise.
     * @param minPrice the minimum price for filtering products; may be null.
     * @param maxPrice the maximum price for filtering products; may be null.
     * @param pageable the pagination information, including page number, size, and sorting details.
     * @return a Page containing products that match the given criteria with their associated categories included.
     */
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