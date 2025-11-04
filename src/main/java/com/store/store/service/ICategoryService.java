package com.store.store.service;

import com.store.store.dto.CategoryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interface defining the contract for managing product categories.
 * Provides methods to perform CRUD operations, retrieve category information,
 * and manage associated resources such as category icons.
 *
 * @author Kardigué
 * @version 3.0 (JWT + Refresh Token + Cookies)
 * @since 2025-11-01
 */
public interface ICategoryService {

    // LECTURE (READ)
    List<CategoryDto> getAllActiveCategories();
    List<CategoryDto> getAllCategories();
    CategoryDto getCategoryByCode(String code);
    CategoryDto getCategoryById(Long id);
    List<CategoryDto> getCategoriesWithProducts();

    // CRÉATION (CREATE)
    CategoryDto createCategory(CategoryDto dto);

    // MISE À JOUR (UPDATE)
    CategoryDto updateCategory(Long id, CategoryDto dto);
    CategoryDto toggleCategoryStatus(Long id);

    // SUPPRESSION (DELETE)
    void deleteCategory(Long id);

    // UPLOAD D'ICÔNE
    String uploadCategoryIcon(Long categoryId, MultipartFile iconFile);
}