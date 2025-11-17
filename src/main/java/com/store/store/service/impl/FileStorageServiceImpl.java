package com.store.store.service.impl;

import com.store.store.exception.ExceptionFactory;
import com.store.store.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageServiceImpl {

    @Value("${store.file.directory}")
    private String uploadDir;

    private Path fileStorageLocation;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @PostConstruct
    public void init() {
        try {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path productsMainPath = fileStorageLocation.resolve("products").resolve("main");
            Path productsGalleryPath = fileStorageLocation.resolve("products").resolve("gallery");

            Files.createDirectories(productsMainPath);
            Files.createDirectories(productsGalleryPath);

            /*log.info("=================================================");
            log.info("FILE STORAGE INITIALIZED");
            log.info("Base: {}", this.fileStorageLocation);
            log.info("Main: {}", productsMainPath);
            log.info("Gallery: {}", productsGalleryPath);
            log.info("=================================================");*/

        } catch (IOException e) {
            log.error("Failed to create storage directory: {}", uploadDir, e);
            throw new FileStorageException(messageService.getMessage("error.file.storage.init.failed", uploadDir), e);
        }
    }

    public String storeGalleryImage(MultipartFile file) throws IOException {
        validateFile(file);
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String fileName = generateUniqueFileName(fileExtension, "product");

        Path targetLocation = this.fileStorageLocation.resolve("products").resolve("gallery").resolve(fileName);

        Files.createDirectories(targetLocation.getParent());
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        log.info("GALLERY image stored: {}", fileName);
        return String.format("/uploads/products/gallery/%s", fileName);
    }

    public String storeProductImage(MultipartFile file) throws IOException {
        validateFile(file);
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String fileName = generateUniqueFileName(fileExtension, "product");

        Path targetLocation = this.fileStorageLocation.resolve("products").resolve("main").resolve(fileName);

        Files.createDirectories(targetLocation.getParent());
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        log.info("MAIN image stored: {}", fileName);
        return String.format("/uploads/products/main/%s", fileName);
    }

    public void deleteProductImage(String imageUrl) {

        if (imageUrl == null || imageUrl.isEmpty()) {return;}

        try {
            String fileName = extractFileNameFromUrl(imageUrl);
            String subFolder = imageUrl.contains("/gallery/") ? "gallery" : "main";

            log.info("File name: {}", fileName);
            log.info("Subfolder: {}", subFolder);

            Path filePath = this.fileStorageLocation.resolve("products").resolve(subFolder).resolve(fileName).normalize();

            log.info("Full path: {}", filePath);
            log.info("Exists: {}", Files.exists(filePath));

            if (!filePath.startsWith(this.fileStorageLocation)) {
                log.warn("Security: Path traversal attempt blocked");
                return;
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Image deleted successfully");
            } else {
                log.warn(" File not found");
            }

        } catch (IOException e) {
            log.error("Delete failed: {}", e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exceptionFactory.validationError("file", messageService.getMessage("error.file.empty"));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw exceptionFactory.validationError("file", messageService.getMessage("error.file.too.large", MAX_FILE_SIZE / 1024 / 1024));
        }
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw exceptionFactory.validationError("file", messageService.getMessage("error.file.invalid.extension", String.join(", ", ALLOWED_EXTENSIONS)));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw exceptionFactory.validationError("file", messageService.getMessage("error.file.not.image"));
        }
    }

    private String generateUniqueFileName(String extension, String prefix) {
        return String.format("%s_%s.%s", prefix, UUID.randomUUID(), extension.toLowerCase());
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "jpg";
        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex > 0 && dotIndex < fileName.length() - 1) ? fileName.substring(dotIndex + 1).toLowerCase() : "jpg";
    }

    private String extractFileNameFromUrl(String imageUrl) {
        // Gérer URL complète
        if (imageUrl.startsWith("http")) {
            int uploadsIndex = imageUrl.indexOf("/uploads/");
            if (uploadsIndex >= 0) {
                imageUrl = imageUrl.substring(uploadsIndex);
            }
        }

        // Extraire le nom de fichier
        int lastSlash = imageUrl.lastIndexOf("/");
        return lastSlash >= 0 ? imageUrl.substring(lastSlash + 1) : imageUrl;
    }
}