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

    List<Product> findByCategoryIgnoreCase(String category);
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);
    Page<Product> findAll(Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
    Page<Product> findByNameContainingIgnoreCase(String query, Pageable pageable);
    // Recherche par catégorie ET nom
    @Query("SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Product> findByCategoryAndNameContainingIgnoreCase(
            @Param("category") String category,
            @Param("query") String query,
            Pageable pageable);
}