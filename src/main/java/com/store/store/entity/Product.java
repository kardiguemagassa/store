package com.store.store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité Product représentant un produit dans l'application e-commerce.
 *
 * @author Kardigué
 * @version 2.0 - Ajout du champ SKU
 * @since 2025-11-08
 */
@Getter
@Setter
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_sku", columnList = "sku", unique = true),
        @Index(name = "idx_category_id", columnList = "category_id"),
        @Index(name = "idx_is_active", columnList = "is_active"),
        @Index(name = "idx_popularity", columnList = "popularity")
})
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "popularity", nullable = false)
    private Integer popularity = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

     //SKU (Stock Keeping Unit) Code unique d'identification du produit.Format recommandé: STK-{CATEGORY}-{NUMBER}
     //Exemple: STK-SPORTS-001, STK-CODING-012
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    /**
     * Galerie d'images avec ordre d'affichage.
     *
     * Permet de stocker plusieurs images pour un produit
     * avec un ordre d'affichage défini.
     */
    @ElementCollection
    @CollectionTable(
            name = "product_gallery_images",
            joinColumns = @JoinColumn(name = "product_id"),
            indexes = {
                    @Index(name = "idx_product_id", columnList = "product_id"),
                    @Index(name = "idx_display_order", columnList = "display_order")
            }
    )
    @OrderColumn(name = "display_order")
    @Column(name = "image_url", length = 500)
    private List<String> galleryImages = new ArrayList<>();


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // HELPER METHODS - CATEGORY

    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }


    public String getCategoryCode() {
        return category != null ? category.getCode() : null;
    }

    // HELPER METHODS - GALLERY IMAGES

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

    public boolean moveGalleryImage(int fromPosition, int toPosition) {
        if (this.galleryImages == null ||
                fromPosition < 0 || fromPosition >= this.galleryImages.size() ||
                toPosition < 0 || toPosition >= this.galleryImages.size()) {
            return false;
        }

        String image = this.galleryImages.remove(fromPosition);
        this.galleryImages.add(toPosition, image);
        return true;
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
            return this.galleryImages.getFirst();
        }
        return null;
    }

    // HELPER METHODS - SKU


    public boolean hasValidSku() {
        return this.sku != null && !this.sku.trim().isEmpty();
    }

    public String generateSku() {
        if (this.id == null) {
            throw new IllegalStateException("Cannot generate SKU: Product ID is null");
        }

        String categoryCode = getCategoryCode();
        if (categoryCode == null) {
            categoryCode = "GENERAL";
        }

        return String.format("STK-%s-%03d", categoryCode, this.id);
    }

    // HELPER METHODS - STOCK

    public boolean isInStock() {
        return this.stockQuantity != null && this.stockQuantity > 0;
    }

    public boolean isOutOfStock() {
        return this.stockQuantity == null || this.stockQuantity == 0;
    }


    public boolean isLowStock() {
        return this.stockQuantity != null && this.stockQuantity > 0 && this.stockQuantity < 10;
    }

    public boolean decreaseStock(int quantity) {
        if (this.stockQuantity == null || this.stockQuantity < quantity) {
            return false;
        }
        this.stockQuantity -= quantity;
        return true;
    }

    public void increaseStock(int quantity) {
        if (this.stockQuantity == null) {
            this.stockQuantity = 0;
        }
        this.stockQuantity += quantity;
    }
}