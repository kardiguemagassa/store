package com.store.store.service.impl;

import com.store.store.exception.FileStorageException;
import com.store.store.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation for handling file storage operations, primarily for product images.
 * Handles functionalities such as storing, validating, and deleting image files,
 * with configurable storage directory and allowed file extensions.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@Service
@Slf4j
public class FileStorageServiceImpl {

    private final Path fileStorageLocation;
    private final List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp", "gif");

    /**
     * Constructs a new instance of FileStorageServiceImpl and initializes the file storage location.
     * Creates directories if they don't exist at the specified location.
     *
     * @param uploadDir the directory path where files will be stored. This is injected from the application properties
     *                  and defaults to "./uploads" if not specified.
     * @throws FileStorageException if the storage directory cannot be created.
     */
    public FileStorageServiceImpl(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        try {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);
            log.info("Dossier de stockage configuré: {}", this.fileStorageLocation);

        } catch (IOException e) {
            throw new FileStorageException("Impossible de créer le dossier de stockage: " + uploadDir, e);
        }
    }

    /**
     * Stores an image file associated with a product in a predefined storage location.
     * Validates the provided file, generates a unique name for it, and saves it under
     * a "products" directory. The file can then be accessed via a URL referencing its
     * storage location.
     *
     * @param file the image file to store, provided as a {@code MultipartFile}.
     *             Must be a valid, non-empty image file within supported extensions
     *             and under the maximum allowed size.
     * @return the relative path to the stored file as a {@code String}. This path can
     *         be used to reference or retrieve the stored image.
     * @throws IOException if an error occurs during file storage, such as issues with
     *         creating directories or writing the file.
     */
    public String storeProductImage(MultipartFile file) throws IOException {

        validateFile(file);

        String fileExtension = getFileExtension(file.getOriginalFilename());
        String fileName = generateUniqueFileName(fileExtension, "product");
        Path targetLocation = this.fileStorageLocation.resolve("products").resolve(fileName);

        Files.createDirectories(targetLocation.getParent());
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        log.info("Image sauvegardée: {}", fileName);
        return String.format("/uploads/products/%s", fileName);
    }

    /**
     * Deletes a product image file from the storage location. Validates the provided image URL,
     * extracts the file name, and ensures security by checking the file path before attempting deletion.
     *
     * @param imageUrl the URL of the image to be deleted. Must be a valid, non-null, and non-empty
     *                 string referencing an existing file within the designated "products" directory.
     */
    public void deleteProductImage(String imageUrl) {

        if (imageUrl == null || imageUrl.isEmpty()) return;

        try {
            String fileName = extractFileNameFromUrl(imageUrl);
            Path filePath = this.fileStorageLocation.resolve("products").resolve(fileName).normalize();

            // Sécurité : vérifier que le chemin est bien dans le dossier autorisé
            if (!filePath.startsWith(this.fileStorageLocation)) {
                log.warn("Tentative d'accès à un chemin non autorisé: {}", filePath);
                return;
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Image supprimée: {}", fileName);
            }
        } catch (IOException e) {
            log.warn("Impossible de supprimer l'image: {}", imageUrl, e);
        }
    }

    /**
     * Validates the provided file by ensuring it meets specific criteria including being
     * non-null, non-empty, within a maximum size limit, having a valid file extension,
     * and being an image type with an appropriate MIME type.
     *
     * @param file the file to validate. Must be a non-null {@code MultipartFile},
     *             not empty, a valid image type, and have an allowed file extension.
     *             The size must not exceed 5MB.
     * @throws ValidationException if the file is null, empty, exceeds the size limit,
     *                              has an unsupported extension, or is not a valid image type.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            //constructeur avec field + message
            throw new ValidationException("file", "Le fichier est vide");
        }

        // 5MB
        long maxFileSize = 5 * 1024 * 1024;
        if (file.getSize() > maxFileSize) {
            throw new ValidationException("file",
                    "Fichier trop volumineux. Maximum: " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new ValidationException("file",
                    "Format non supporté. Utilisez: " + String.join(", ", allowedExtensions));
        }

        // Validation du type MIME
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ValidationException("file", "Le fichier doit être une image valide");
        }
    }

    /**
     * Generates a unique file name using a random UUID and specified parameters.
     * The file name is created by combining the provided prefix, a unique identifier,
     * and the specified file extension in lowercase.
     *
     * @param extension the file extension to be used for the file name. This must be a valid,
     *                  non-null, and non-empty string representing the file type, e.g., "jpg", "png".
     * @param prefix    a custom prefix to prepend to the generated unique file name. This can
     *                  be used to categorize or identify the file context.
     * @return a unique file name as a {@code String}, constructed by combining the prefix,
     *         a randomly generated UUID, and the file extension in lowercase.
     */
    private String generateUniqueFileName(String extension, String prefix) {
        return String.format("%s_%s.%s",

                UUID.randomUUID().toString(),
                extension.toLowerCase()
        );
    }

    /**
     * Extracts the file extension from the given file name. If the file name is null or
     * does not contain a valid file extension, the method returns "jpg" as the default extension.
     *
     * @param fileName the name of the file as a string. This may include the full path and file extension.
     *                 If null, the method returns the default extension "jpg".
     * @return the file extension extracted from the file name in lowercase. If the file name
     *         does not contain an extension or is null, it returns "jpg" as the default.
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) return "jpg";

        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex > 0 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1).toLowerCase()
                : "jpg";
    }

    /**
     * Extracts the file name from a given URL string. The method identifies the final segment
     * of the URL path after the last '/' character and returns it as the file name.
     *
     * @param imageUrl the URL string from which the file name is to be extracted. Must be a non-null,
     *                 valid string representing a file path or URL.
     * @return the file name as a String extracted from the given URL. If the URL does not contain
     *         a valid path, an empty string is returned.
     */
    private String extractFileNameFromUrl(String imageUrl) {
        // Extrait "filename.jpg" depuis "/uploads/products/filename.jpg"
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }
}