package com.store.store.service;

import com.store.store.dto.CategoryDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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