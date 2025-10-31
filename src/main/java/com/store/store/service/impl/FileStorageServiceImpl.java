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

@Service
@Slf4j
public class FileStorageServiceImpl {

    private final Path fileStorageLocation;
    private final List<String> allowedExtensions = List.of("jpg", "jpeg", "png", "webp", "gif");

    public FileStorageServiceImpl(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        try {
            this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.fileStorageLocation);
            log.info("Dossier de stockage configuré: {}", this.fileStorageLocation);

        } catch (IOException e) {
            throw new FileStorageException("Impossible de créer le dossier de stockage: " + uploadDir, e);
        }
    }

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

    private String generateUniqueFileName(String extension, String prefix) {
        return String.format("%s_%s.%s",

                UUID.randomUUID().toString(),
                extension.toLowerCase()
        );
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "jpg";

        int dotIndex = fileName.lastIndexOf(".");
        return (dotIndex > 0 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1).toLowerCase()
                : "jpg";
    }

    private String extractFileNameFromUrl(String imageUrl) {
        // Extrait "filename.jpg" depuis "/uploads/products/filename.jpg"
        return imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
    }
}