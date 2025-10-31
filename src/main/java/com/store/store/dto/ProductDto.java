package com.store.store.dto;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDto {

    private Long productId;
    private Integer stockQuantity;

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 250, message = "Le nom ne doit pas dépasser 250 caractères")
    private String name;

    @NotBlank(message = "La description est obligatoire")
    @Size(max = 500, message = "La description ne doit pas dépasser 500 caractères")
    private String description;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0")
    private BigDecimal price;

    @Min(value = 0, message = "La popularité doit être positive")
    private Integer popularity;

    //  NOUVEAU: Galerie d'images supplémentaires
    private List<String> galleryImages = new ArrayList<>();

    @Size(max = 500, message = "L'URL de l'image ne doit pas dépasser 500 caractères")
    private String imageUrl;

    private Boolean isActive = true;

    // NOUVEAU : Relation avec Category
    @NotNull(message = "La catégorie est obligatoire")
    private Long categoryId;

    // Champs supplémentaires pour l'affichage (non modifiables)
    private String categoryCode;
    private String categoryName;
    private String categoryIcon;

    // =====================================================
    // MÉTHODES UTILITAIRES POUR LA GALERIE
    // =====================================================

    public void addGalleryImage(String imageUrl) {
        if (this.galleryImages == null) {
            this.galleryImages = new ArrayList<>();
        }
        this.galleryImages.add(imageUrl);
    }

    public void addGalleryImage(String imageUrl, int position) {
        if (this.galleryImages == null) {
            this.galleryImages = new ArrayList<>();
        }
        if (position >= 0 && position <= this.galleryImages.size()) {
            this.galleryImages.add(position, imageUrl);
        } else {
            this.galleryImages.add(imageUrl);
        }
    }

    public boolean removeGalleryImage(String imageUrl) {
        if (this.galleryImages != null) {
            return this.galleryImages.remove(imageUrl);
        }
        return false;
    }

    public String removeGalleryImage(int position) {
        if (this.galleryImages != null && position >= 0 && position < this.galleryImages.size()) {
            return this.galleryImages.remove(position);
        }
        return null;
    }

    public void clearGalleryImages() {
        if (this.galleryImages != null) {
            this.galleryImages.clear();
        }
    }

    public int getGalleryImagesCount() {
        return this.galleryImages != null ? this.galleryImages.size() : 0;
    }

    public boolean hasGalleryImage(String imageUrl) {
        return this.galleryImages != null && this.galleryImages.contains(imageUrl);
    }

    public String getFirstGalleryImage() {
        if (this.galleryImages != null && !this.galleryImages.isEmpty()) {
            return this.galleryImages.get(0);
        }
        return null;
    }

    /**
     * ✅ URL de l'image principale (priorité à imageUrl, puis première de la galerie)
     */
    public String getMainImageUrl() {
        if (this.imageUrl != null && !this.imageUrl.trim().isEmpty()) {
            return this.imageUrl;
        }
        return getFirstGalleryImage();
    }

    /**
     * ✅ TOUTES les images (principale + galerie)
     */
    public List<String> getAllImages() {
        List<String> allImages = new ArrayList<>();

        // Image principale
        if (this.imageUrl != null && !this.imageUrl.trim().isEmpty()) {
            allImages.add(this.imageUrl);
        }

        // Images de la galerie
        if (this.galleryImages != null) {
            allImages.addAll(this.galleryImages);
        }

        return allImages;
    }
}