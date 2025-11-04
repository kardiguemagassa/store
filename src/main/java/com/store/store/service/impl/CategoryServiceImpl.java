package com.store.store.service.impl;

import com.store.store.dto.CategoryDto;
import com.store.store.entity.Category;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.exception.ResourceNotFoundException;
import com.store.store.repository.CategoryRepository;
import com.store.store.repository.ProductRepository;
import com.store.store.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
 * Implementation of the {@code ICategoryService} interface for managing product categories.
 * Provides functionality for CRUD operations, retrieving category data, managing associated
 * product relationships, and handling category icons.
 *
 * @author Kardigué
 * @version 3.0 (JWT + Refresh Token + Cookies)
 * @since 2025-11-01
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

    // LECTURE (READ)

    /**
     * Retrieves all active categories from the repository, ordered by their
     * display order in ascending order, and maps them to DTOs.
     *
     * The method filters categories where the "isActive" flag is true and
     * transforms each category entity into a CategoryDto representation. It logs
     * the actions and throws a business exception in case of database access issues.
     *
     * @return a list of {@link CategoryDto} objects representing all active categories
     */
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
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.fetch.failed")
            );
        }
    }

    /**
     * Retrieves all categories from the repository, including both active and inactive ones,
     * and maps them to DTO objects.
     *
     * This method fetches all category entities available in the database, transforms them
     * into {@link CategoryDto} representations, and returns the resulting list. In case
     * of a database access issue, a business exception is thrown.
     *
     * @return a list of {@link CategoryDto} objects representing all categories.
     */
    @Override
    public List<CategoryDto> getAllCategories() {
        try {
            log.info("Fetching all categories (including inactive)");
            List<Category> categories = categoryRepository.findAll();

            return categories.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching all categories", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.fetch.all.failed")
            );
        }
    }

    /**
     * Retrieves a category by its unique code and maps it to a {@link CategoryDto}.
     *
     * The method fetches the category matching the provided code from the repository.
     * It validates the input code, ensures the category exists, and handles exceptions
     * related to database access or resource not found scenarios. The resulting category
     * is converted into a {@link CategoryDto} representation before returning.
     *
     * @param code the unique code representing the category to be retrieved
     * @return a {@link CategoryDto} object representing the category with the specified code
     * @throws IllegalArgumentException if the provided code is invalid (e.g., null or empty)
     * @throws ResourceNotFoundException if no category is found with the given code
     * @throws BusinessException if a database error occurs during the operation
     */
    @Override
    public CategoryDto getCategoryByCode(String code) {
        try {
            log.info("Fetching category by code: {}", code);
            validateCategoryCode(code);

            Category category = categoryRepository.findByCode(code)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "code", code
                    ));

            log.info("Category found: {} (code: {})", category.getName(), code);
            return toDto(category);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching category by code: {}", code, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.fetch.byCode.failed", code)
            );
        }
    }

    /**
     * Retrieves a category by its unique identifier and maps it to a {@link CategoryDto}.
     *
     * The method fetches the category with the specified ID from the repository. It validates the
     * input ID, ensures the category exists, and handles exceptions for cases such as database access
     * issues or missing resources. The resulting category is transformed into a {@link CategoryDto}
     * before being returned.
     *
     * @param id the unique identifier of the category to be retrieved
     * @return a {@link CategoryDto} object representing the category with the specified ID
     * @throws IllegalArgumentException if the provided ID is null or invalid (e.g., non-positive value)
     * @throws ResourceNotFoundException if no category is found with the given ID
     * @throws BusinessException if a database error occurs during the operation
     */
    @Override
    public CategoryDto getCategoryById(Long id) {
        try {
            log.info("Fetching category by ID: {}", id);
            validateCategoryId(id);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "id", id.toString()
                    ));

            log.info("Category found: {} (ID: {})", category.getName(), id);
            return toDto(category);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while fetching category by ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.fetch.byId.failed")
            );
        }
    }

    /**
     * Retrieves a list of categories that contain at least one associated product.
     *
     * The method fetches categories that are active and have related products from the database,
     * maps them to {@link CategoryDto} objects, and returns them. In case of a data access
     * issue, a business exception is thrown.
     *
     * @return a list of {@link CategoryDto} objects representing categories with associated products
     * @throws BusinessException if a database access error occurs during the operation
     */
    @Override
    public List<CategoryDto> getCategoriesWithProducts() {
        try {
            log.info("Fetching categories that have products");
            List<Category> categories = categoryRepository.findCategoriesWithProducts();

            return categories.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching categories with products", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.fetch.withProducts.failed")
            );
        }
    }

    // CRÉATION (CREATE)

    /**
     * Creates a new category based on the provided {@link CategoryDto}.
     *
     * The method validates the input data, checks whether a category with the given
     * code already exists, and then saves the new category. If the operation is
     * successful, the created category is returned as a {@link CategoryDto}.
     *
     * @param dto the {@link CategoryDto} containing the details of the category to create
     * @return a {@link CategoryDto} representing the newly created category
     * @throws BusinessException if a business constraint is violated, such as a duplicate code
     * @throws DataAccessException if a database access error occurs
     */
    @Transactional
    @Override
    public CategoryDto createCategory(CategoryDto dto) {
        try {
            log.info("Creating new category: {}", dto.getName());
            validateCategoryForCreation(dto);

            // Vérifier si le code existe déjà
            if (categoryRepository.existsByCode(dto.getCode().toUpperCase())) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.category.code.already.exists", dto.getCode())
                );
            }

            Category category = new Category();
            category.setCode(dto.getCode().toUpperCase());
            category.setName(dto.getName());
            category.setDescription(dto.getDescription());
            category.setIcon(dto.getIcon() != null ? dto.getIcon() : "📦");
            category.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 999);
            category.setIsActive(true);

            Category saved = categoryRepository.save(category);
            log.info("Category created successfully with ID: {}", saved.getCategoryId());

            return toDto(saved);

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while creating category", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.create.failed")
            );
        }
    }

    // MISE À JOUR (UPDATE)

    /**
     * Updates an existing category with the provided details.
     *
     * The method validates the input data, ensures that the specified category exists,
     * and checks for any conflicting data such as duplicate codes. It applies the updates
     * to the category's fields and persists the changes.
     * Handles database errors and other business-related exceptions appropriately.
     *
     * @param id the unique identifier of the category to be updated
     * @param dto the {@link CategoryDto} object containing the updated data for the category
     * @return a {@link CategoryDto} representing the updated category
     * @throws IllegalArgumentException if the provided ID or DTO is null or invalid
     * @throws ResourceNotFoundException if no category is found with the given ID
     * @throws BusinessException if a database access error occurs or a business constraint is violated
     */
    @Transactional
    @Override
    public CategoryDto updateCategory(Long id, CategoryDto dto) {
        try {
            log.info("Updating category ID: {}", id);
            validateCategoryId(id);
            validateCategoryForUpdate(dto);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "id", id.toString()
                    ));

            // Vérifier si le nouveau code existe déjà (sauf si c'est le même)
            if (!category.getCode().equals(dto.getCode().toUpperCase()) &&
                    categoryRepository.existsByCode(dto.getCode().toUpperCase())) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.category.code.already.exists", dto.getCode())
                );
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

            Category updated = categoryRepository.save(category);
            log.info("Category updated successfully: {}", updated.getName());

            return toDto(updated);

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while updating category ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.update.failed")
            );
        }
    }

    /**
     * Toggles the active status of a category identified by its ID.
     *
     * This method retrieves the category with the specified ID from the repository,
     * inverses its "isActive" status, and saves the updated category back to the database.
     * If the category does not exist or a database error occurs, it handles the exception
     * appropriately by either re-throwing it or wrapping it in a business exception.
     *
     * @param id the unique identifier of the category whose status is to be toggled
     * @return a {@link CategoryDto} object representing the updated category
     * @throws IllegalArgumentException if the provided ID is invalid (e.g., null or non-positive)
     * @throws ResourceNotFoundException*/
    @Transactional
    @Override
    public CategoryDto toggleCategoryStatus(Long id) {
        try {
            log.info("Toggling status for category ID: {}", id);
            validateCategoryId(id);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "id", id.toString()
                    ));

            category.setIsActive(!category.getIsActive());
            Category updated = categoryRepository.save(category);

            log.info("Category status toggled: {} is now {}",
                    updated.getName(), updated.getIsActive() ? "active" : "inactive");

            return toDto(updated);

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while toggling category status ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.toggle.failed")
            );
        }
    }

    // SUPPRESSION (DELETE)

    /**
     * Deletes a category by its ID.
     * This method validates the category ID, checks for associated products,
     * and removes the category if it exists and has no associated products.
     *
     * @param id the unique identifier of the category to be deleted
     *           (must not be null)
     * @throws ResourceNotFoundException if the category with the given ID does not*/
    @Transactional
    @Override
    public void deleteCategory(Long id) {
        try {
            log.info("Deleting category ID: {}", id);
            validateCategoryId(id);

            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "id", id.toString()
                    ));

            // CORRIGÉ : Utilisez countByCategoryId (votre méthode existante)
            Long productCount = productRepository.countByCategoryId(id);
            if (productCount > 0) {
                throw exceptionFactory.businessError(
                        getLocalizedMessage("error.category.delete.hasProducts",
                                category.getName(), productCount)
                );
            }

            categoryRepository.delete(category);
            log.info("Category deleted successfully: {}", category.getName());

        } catch (ResourceNotFoundException | BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while deleting category ID: {}", id, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.delete.failed")
            );
        }
    }

    // UPLOAD D'ICÔNE

    /**
     * Uploads an icon file for the specified category, updates the category with the URL
     * of the uploaded icon, and saves the changes in the database. Ensures that the provided
     * category ID and file are valid before proceeding.
     *
     * @param categoryId the ID of the category to which the icon should be assigned
     * @param iconFile the image file to be uploaded as the icon for the category
     * @return the URL of the uploaded icon file
     * @throws ResourceNotFoundException if the category with the specified ID does not exist
     * @throws DataAccessException if a database error occurs while updating the category
     * @throws IOException if an error occurs while saving the icon file to the file system
     */
    @Transactional
    @Override
    public String uploadCategoryIcon(Long categoryId, MultipartFile iconFile) {
        try {
            log.info("Uploading icon for category ID: {}", categoryId);
            validateCategoryId(categoryId);
            validateIconFile(iconFile);

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Category", "id", categoryId.toString()
                    ));

            // Générer un nom de fichier unique
            String fileName = generateUniqueIconFileName(iconFile.getOriginalFilename());

            // Sauvegarder le fichier
            String iconUrl = saveIconFile(iconFile, fileName);

            // Mettre à jour la catégorie
            category.setIcon(iconUrl);
            categoryRepository.save(category);

            log.info("Icon uploaded successfully for category: {}", category.getName());
            return iconUrl;

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Database error while uploading icon for category ID: {}", categoryId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.icon.upload.failed")
            );
        } catch (IOException e) {
            log.error("File system error while uploading icon for category ID: {}", categoryId, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.category.icon.save.failed")
            );
        }
    }

    // VALIDATION

    /**
     * Validates the provided category ID to ensure it is not null and greater than zero.
     * Throws a validation error if the ID is invalid.
     *
     * @param id the category ID to validate
     */
    private void validateCategoryId(Long id) {
        if (id == null || id <= 0) {
            throw exceptionFactory.validationError("categoryId",
                    getLocalizedMessage("validation.category.id.invalid"));
        }
    }

    /**
     * Validates the given category code to ensure it meets the required constraints.
     * The method checks for null or empty values and ensures the code does not exceed
     * the maximum allowable length.
     *
     * @param code the category code to be validated. Must not be null, empty, or exceed
     *             50 characters in length.
     */
    private void validateCategoryCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw exceptionFactory.validationError("code",
                    getLocalizedMessage("validation.category.code.required"));
        }

        if (code.length() > 50) {
            throw exceptionFactory.validationError("code",
                    getLocalizedMessage("validation.category.code.tooLong", 50));
        }
    }

    /**
     * Validates the given {@code CategoryDto} object to ensure it meets the
     * requirements for category creation. Throws a validation error if any
     * criteria are not met.
     *
     * @param dto the {@code CategoryDto} object that represents the category
     *            to be validated. It must not be null and should contain a
     *            valid code and name. The code must not exceed 50 characters,
     *            and the name must not exceed 100 characters.
     */
    private void validateCategoryForCreation(CategoryDto dto) {
        if (dto == null) {
            throw exceptionFactory.validationError("categoryDto",
                    getLocalizedMessage("validation.category.create.required"));
        }

        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            throw exceptionFactory.validationError("code",
                    getLocalizedMessage("validation.category.code.required"));
        }

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw exceptionFactory.validationError("name",
                    getLocalizedMessage("validation.category.name.required"));
        }

        if (dto.getCode().length() > 50) {
            throw exceptionFactory.validationError("code",
                    getLocalizedMessage("validation.category.code.tooLong", 50));
        }

        if (dto.getName().length() > 100) {
            throw exceptionFactory.validationError("name",
                    getLocalizedMessage("validation.category.name.tooLong", 100));
        }
    }

    /**
     * Validates the given category data transfer object (DTO) for the update operation.
     * This method ensures that the necessary validation checks are performed
     * before updating an existing category.
     *
     * @param dto the {@code CategoryDto} object containing category data to validate
     */
    private void validateCategoryForUpdate(CategoryDto dto) {
        validateCategoryForCreation(dto);
    }

    /**
     * Validates the provided icon file for non-nullity, file type, and size constraints.
     * Throws a validation error if any of the validations fail.
     *
     * @param iconFile the icon file to validate. Must not be null, empty, or exceed the size limit of 2 MB.
     *                 Additionally, it should have a valid MIME type for an icon file.
     */
    private void validateIconFile(MultipartFile iconFile) {
        if (iconFile == null || iconFile.isEmpty()) {
            throw exceptionFactory.validationError("iconFile",
                    getLocalizedMessage("validation.category.icon.required"));
        }

        if (!isValidIconType(iconFile.getContentType())) {
            throw exceptionFactory.validationError("iconFile",
                    getLocalizedMessage("validation.category.icon.type.invalid"));
        }

        if (iconFile.getSize() > 2 * 1024 * 1024) {
            throw exceptionFactory.validationError("iconFile",
                    getLocalizedMessage("validation.category.icon.size.tooLarge", 2));
        }
    }

    /**
     * Validates if the provided content type corresponds to a supported icon type.
     *
     * @param contentType the content type to validate as a string
     * @return true if the content type matches one of the supported icon types (image/jpeg, image/png, image/svg+xml, image/webp), otherwise false
     */
    private boolean isValidIconType(String contentType) {
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/svg+xml") ||
                        contentType.equals("image/webp")
        );
    }

    // MÉTHODES UTILITAIRES

    /**
     * Generates a unique file name for an icon by appending a timestamp
     * and preserving the original file's extension.
     *
     * @param originalFileName the original file name, used to extract the file extension
     * @return a unique file name in the format "category_icon_<timestamp><fileExtension>"
     */
    private String generateUniqueIconFileName(String originalFileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileExtension = originalFileName != null ?
                originalFileName.substring(originalFileName.lastIndexOf(".")) : ".png";
        return "category_icon_" + timestamp + fileExtension;
    }

    /**
     * Saves the icon file to the specified upload directory with the given file name.
     *
     * @param iconFile the MultipartFile representing the icon to be saved
     * @param fileName the name under which the file should be stored
     * @return the relative path to the saved file
     * @throws IOException if an I/O error occurs during file creation or copying
     */
    private String saveIconFile(MultipartFile iconFile, String fileName) throws IOException {
        Path uploadPath = Paths.get("uploads/categories/icons");
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(iconFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/categories/icons/" + fileName;
    }

    /**
     * Converts a {@link Category} entity to a {@link CategoryDto}.
     *
     * @param category the {@link Category} entity to be converted
     * @return a {@link CategoryDto} containing the mapped data from the source {@link Category}
     */
    private CategoryDto toDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setCategoryId(category.getCategoryId());
        dto.setCode(category.getCode());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setIcon(category.getIcon());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setIsActive(category.getIsActive());

        Long count = productRepository.countByCategoryId(category.getCategoryId());
        dto.setProductCount(count);

        return dto;
    }

    /**
     * Retrieves the localized message for the given message code and arguments based on the current locale.
     *
     * @param code the message code to look up, such as an error code or message key
     * @param args optional arguments to format the message, can be empty or null if not required
     * @return the localized message as a String based on the provided code and arguments
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}