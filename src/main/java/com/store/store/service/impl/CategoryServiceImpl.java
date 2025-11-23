package com.store.store.service.impl;

import com.store.store.dto.category.CategoryDto;
import com.store.store.entity.Category;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.repository.CategoryRepository;
import com.store.store.repository.ProductRepository;
import com.store.store.service.ICategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec MessageService
 * @since 2025-01-06
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    // LECTURE (READ)
    @Override
    public List<CategoryDto> getAllActiveCategories() {
        try {
            log.info("Fetching all active categories");
            List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();

            log.info("Found {} active categories", categories.size());
            return categories.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching active categories", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.category.fetch.failed"));
        }
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        try {
            log.info("Fetching all categories (including inactive)");
            List<Category> categories = categoryRepository.findAll();

            log.info("Found {} categories total", categories.size());
            return categories.stream().map(this::toDto).collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching all categories", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.category.fetch.all.failed"));
        }
    }

    @Override
    public CategoryDto getCategoryByCode(String code) {
        try {
            log.info("Fetching category by code: {}", code);
            validateCategoryCode(code);

            Category category = categoryRepository.findByCode(code)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Category", "code", code));

            log.info("Category found: {} (code: {})", category.getName(), code);
            return toDto(category);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching category by code: {}", code, e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.category.fetch.byCode.failed", code));
        }
    }

    @Override
    public CategoryDto getCategoryById(Long id) {
        try {
            log.info("Fetching category by ID: {}", id);
            validateCategoryId(id);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Category", "id", id.toString()));

            log.info("Category found: {} (ID: {})", category.getName(), id);
            return toDto(category);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching category by ID: {}", id, e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.category.fetch.byId.failed"));
        }
    }

    @Override
    public List<CategoryDto> getCategoriesWithProducts() {
        try {
            log.info("Fetching categories that have products");
            List<Category> categories = categoryRepository.findCategoriesWithProducts();

            log.info("Found {} categories with products", categories.size());
            return categories.stream().map(this::toDto).collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching categories with products", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.category.fetch.withProducts.failed"));
        }
    }

    // CRÉATION (CREATE)
    @Transactional
    @Override
    public CategoryDto createCategory(CategoryDto dto) {
        try {
            log.info("Creating new category: {}", dto.getName());
            validateCategoryForCreation(dto);

            // Vérifier si le code existe déjà
            if (categoryRepository.existsByCode(dto.getCode().toUpperCase())) {
                // Utilisation de messageService
                throw exceptionFactory.businessError(messageService.getMessage("error.category.code.already.exists", dto.getCode()));
            }

            // Création de l'entité
            Category category = new Category();
            category.setCode(dto.getCode().toUpperCase());
            category.setName(dto.getName());
            category.setDescription(dto.getDescription());
            category.setIcon(dto.getIcon() != null ? dto.getIcon() : "📦");
            category.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 999);
            category.setIsActive(true);

            // Sauvegarde
            Category saved = categoryRepository.save(category);
            log.info("Category created successfully with ID: {}", saved.getCategoryId());

            return toDto(saved);

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while creating category", e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.category.create.failed"));
        }
    }

    // MISE À JOUR (UPDATE)
    @Transactional
    @Override
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        try {
            log.info("Updating category ID: {}", id);
            validateCategoryId(id);
            validateCategoryForUpdate(dto);

            // Récupération de la catégorie existante
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Category", "id", id.toString()));

            // Vérifier si le nouveau code existe déjà (sauf si c'est le même)
            if (!category.getCode().equals(dto.getCode().toUpperCase()) &&
                    categoryRepository.existsByCode(dto.getCode().toUpperCase())) {
                // Utilisation de messageService
                throw exceptionFactory.businessError(messageService.getMessage("error.category.code.already.exists", dto.getCode()));
            }

            // Mise à jour des champs
            category.setCode(dto.getCode().toUpperCase());
            category.setName(dto.getName());
            category.setDescription(dto.getDescription());
            category.setIcon(dto.getIcon());
            category.setDisplayOrder(dto.getDisplayOrder());
            if (dto.getIsActive() != null) {
                category.setIsActive(dto.getIsActive());
            }

            // Sauvegarde
            Category updated = categoryRepository.save(category);
            log.info("Category updated successfully: {}", updated.getName());

            return toDto(updated);

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while updating category ID: {}", id, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.category.update.failed"));
        }
    }

    @Transactional
    @Override
    public CategoryDto toggleCategoryStatus(Long id) {
        try {
            log.info("Toggling status for category ID: {}", id);
            validateCategoryId(id);

            // Récupération de la catégorie
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Category", "id", id.toString()));

            // Inversion du statut
            category.setIsActive(!category.getIsActive());
            Category updated = categoryRepository.save(category);

            log.info("Category status toggled: {} is now {}",
                    updated.getName(), updated.getIsActive() ? "active" : "inactive");

            return toDto(updated);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while toggling category status ID: {}", id, e);
            // Utilisation de messageService
            throw exceptionFactory.businessError(messageService.getMessage("error.category.toggle.failed"));
        }
    }

    // SUPPRESSION (DELETE)
    @Transactional
    @Override
    public void deleteCategory(Long id) {
        try {
            log.info("Deleting category ID: {}", id);
            validateCategoryId(id);

            // Récupération de la catégorie
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Category", "id", id.toString()));

            // Comptage des produits associés
            Long productCount = productRepository.countByCategoryId(id);
            if (productCount > 0) {
                // Utilisation de messageService
                throw exceptionFactory.businessError(
                        messageService.getMessage("error.category.delete.hasProducts", category.getName(), productCount));
            }

            // Suppression de la catégorie
            categoryRepository.delete(category);
            log.info("Category deleted successfully: {}", category.getName());

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while deleting category ID: {}", id, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.category.delete.failed"));
        }
    }

    // UPLOAD D'ICÔNE
    @Transactional
    @Override
    public String uploadCategoryIcon(Long categoryId, MultipartFile iconFile) {
        try {
            log.info("Uploading icon for category ID: {}", categoryId);
            validateCategoryId(categoryId);
            validateIconFile(iconFile);

            // Récupération de la catégorie
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound("Category", "id", categoryId.toString()));

            // Génération d'un nom de fichier unique
            String fileName = generateUniqueIconFileName(iconFile.getOriginalFilename());

            // Sauvegarde du fichier
            String iconUrl = saveIconFile(iconFile, fileName);

            // Mise à jour de la catégorie
            category.setIcon(iconUrl);
            categoryRepository.save(category);

            log.info("Icon uploaded successfully for category: {}", category.getName());
            return iconUrl;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while uploading icon for category ID: {}", categoryId, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.category.icon.upload.failed"));
        } catch (IOException e) {
            log.error("File system error while uploading icon for category ID: {}", categoryId, e);
            throw exceptionFactory.businessError(messageService.getMessage("error.category.icon.save.failed"));
        }
    }

    // VALIDATION
    private void validateCategoryId(Long id) {
        if (id == null || id <= 0) {
            throw exceptionFactory.validationError("categoryId", messageService.getMessage("validation.category.id.invalid"));
        }
    }

    private void validateCategoryCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw exceptionFactory.validationError("code", messageService.getMessage("validation.category.code.required"));
        }
        if (code.length() > 50) {
            throw exceptionFactory.validationError("code", messageService.getMessage("validation.category.code.tooLong", 50));
        }
    }

    private void validateCategoryForCreation(CategoryDto dto) {
        if (dto == null) {
            throw exceptionFactory.validationError("categoryDto", messageService.getMessage("validation.category.create.required"));
        }
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw exceptionFactory.validationError("code", messageService.getMessage("validation.category.code.required"));
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw exceptionFactory.validationError("name", messageService.getMessage("validation.category.name.required"));
        }
        if (dto.getCode().length() > 50) {
            throw exceptionFactory.validationError("code", messageService.getMessage("validation.category.code.tooLong", 50));
        }
        if (dto.getName().length() > 100) {
            throw exceptionFactory.validationError("name", messageService.getMessage("validation.category.name.tooLong", 100));
        }
    }

    private void validateCategoryForUpdate(CategoryDto dto) {
        validateCategoryForCreation(dto);
    }

    private void validateIconFile(MultipartFile iconFile) {
        if (iconFile == null || iconFile.isEmpty()) {
            throw exceptionFactory.validationError("iconFile", messageService.getMessage("validation.category.icon.required"));
        }
        if (!isValidIconType(iconFile.getContentType())) {
            throw exceptionFactory.validationError("iconFile", messageService.getMessage("validation.category.icon.type.invalid"));
        }
        if (iconFile.getSize() > 2 * 1024 * 1024) {
            throw exceptionFactory.validationError("iconFile", messageService.getMessage("validation.category.icon.size.tooLarge", 2));
        }
    }

    private boolean isValidIconType(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/svg+xml") ||
                        contentType.equals("image/webp")
        );
    }

    // MÉTHODES UTILITAIRES
    private String generateUniqueIconFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileExtension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf(".")) : ".png";
        return "category_icon_" + timestamp + fileExtension;
    }

    private String saveIconFile(MultipartFile iconFile, String fileName) throws IOException {
        Path uploadPath = Paths.get("uploads/categories/icons");
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(iconFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/categories/icons/" + fileName;
    }

    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIcon(category.getIcon());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setIsActive(category.getIsActive());

        // Calcul du nombre de produits
        Long count = productRepository.countByCategoryId(category.getCategoryId());
        dto.setProductCount(count);

        return dto;
    }
}