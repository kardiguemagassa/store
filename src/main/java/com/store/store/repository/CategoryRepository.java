package com.store.store.repository;

import com.store.store.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCode(String code);
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();
    boolean existsByCode(String code);

    @Query("SELECT DISTINCT c FROM Category c JOIN c.products p WHERE c.isActive = true")
    List<Category> findCategoriesWithProducts();
}