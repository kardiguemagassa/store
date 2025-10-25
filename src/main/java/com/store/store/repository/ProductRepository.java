package com.store.store.repository;

import com.store.store.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Recherche générale
    Page<Product> findAll(Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
    Page<Product> findByNameContainingIgnoreCase(String query, Pageable pageable);

    // Recherche par catégorie ID
    List<Product> findByCategoryCategoryId(Long categoryId);
    Page<Product> findByCategoryCategoryId(Long categoryId, Pageable pageable);

    // Recherche par code de catégorie
    @Query("SELECT p FROM Product p WHERE p.category.code = :code")
    List<Product> findByCategoryCode(@Param("code") String code);

    @Query("SELECT p FROM Product p WHERE p.category.code = :code")
    Page<Product> findByCategoryCode(@Param("code") String code, Pageable pageable);

    // Recherche par catégorie ET nom
    @Query("SELECT p FROM Product p WHERE p.category.code = :categoryCode " +
            "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Product> findByCategoryCodeAndNameContaining(
            @Param("categoryCode") String categoryCode,
            @Param("query") String query,
            Pageable pageable
    );

    // Comptage
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.categoryId = :categoryId")
    Long countByCategoryId(@Param("categoryId") Long categoryId);

    // Recherche avancée
    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);
}