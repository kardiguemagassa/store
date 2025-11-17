package com.store.store.dto.product;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDto {

    private Long productId;
    private Integer stockQuantity;

    @NotBlank(message = "{validation.required}")
    @Size(max = 250, message = "{validation.size.max}")
    private String name;

    @NotBlank(message = "{validation.required}")
    @Size(max = 500, message = "{validation.size.max}")
    private String description;

    @NotNull(message = "{validation.required}")
    @DecimalMin(value = "0.01", message = "{validation.decimal.min}")
    @Digits(integer = 8, fraction = 2, message = "{validation.digits}")
    private BigDecimal price;

    @Min(value = 0, message = "{validation.min.value}")
    private Integer popularity;

    @Size(max = 10, message = "{validation.product.gallery.size}")
    private List<String> galleryImages = new ArrayList<>();

    @Size(max = 500, message = "{validation.size.max}")
    @Pattern(regexp = "^(https?://.*|/images/.*)$", message = "{validation.product.imageUrl.pattern}")
    private String imageUrl;

    private Boolean isActive = true;

    // Relation avec Category
    @NotNull(message = "{validation.required}")
    private Long categoryId;


    private String categoryCode;
    private String categoryName;
    private String categoryIcon;

    // MÉTHODES UTILITAIRES POUR LA GALERIE

    public void addGalleryImage(String imageUrl) {
        if (this.galleryImages == null) {
            this.galleryImages = new ArrayList<>();
        }
        if (this.galleryImages.size() < 10) { // Limite métier
            this.galleryImages.add(imageUrl);
        }
    }

    public void addGalleryImage(String imageUrl, int position) {
        if (this.galleryImages == null) {
            this.galleryImages = new ArrayList<>();
        }
        if (position >= 0 && position <= this.galleryImages.size() && this.galleryImages.size() < 10) {
            this.galleryImages.add(position, imageUrl);
        } else if (this.galleryImages.size() < 10) {
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

    public String getMainImageUrl() {
        if (this.imageUrl != null && !this.imageUrl.trim().isEmpty()) {
            return this.imageUrl;
        }
        return getFirstGalleryImage();
    }

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

    public boolean hasImages() {
        return (imageUrl != null && !imageUrl.trim().isEmpty()) || (galleryImages != null && !galleryImages.isEmpty());
    }

    public boolean isAvailable() {
        return Boolean.TRUE.equals(isActive) && stockQuantity != null && stockQuantity > 0;
    }

    public BigDecimal getPriceWithDiscount(BigDecimal discountPercentage) {
        if (price == null || discountPercentage == null) {
            return price;
        }
        BigDecimal discountAmount = price.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
        return price.subtract(discountAmount).max(BigDecimal.ZERO);
    }
}