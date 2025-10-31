package com.store.store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "products")
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
    private Integer popularity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    // Galerie d'images avec ordre d'affichage
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

    // =====================================================
    // HELPER METHODS - AMÉLIORÉES
    // =====================================================

    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    public String getCategoryCode() {
        return category != null ? category.getCode() : null;
    }

    /**
     * ✅ AJOUTER une image à la galerie (à la fin)
     */
    public void addGalleryImage(String imageUrl) {
        if (this.galleryImages == null) {
            this.galleryImages = new ArrayList<>();
        }
        this.galleryImages.add(imageUrl);
    }

    /**
     * ✅ AJOUTER une image à une position spécifique
     */
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

    /**
     * ✅ SUPPRIMER une image par son URL
     */
    public boolean removeGalleryImage(String imageUrl) {
        if (this.galleryImages != null) {
            return this.galleryImages.remove(imageUrl);
        }
        return false;
    }

    /**
     * ✅ SUPPRIMER une image par sa position
     */
    public String removeGalleryImage(int position) {
        if (this.galleryImages != null && position >= 0 && position < this.galleryImages.size()) {
            return this.galleryImages.remove(position);
        }
        return null;
    }

    /**
     * ✅ DÉPLACER une image dans la galerie
     */
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

    /**
     * ✅ VIDER la galerie
     */
    public void clearGalleryImages() {
        if (this.galleryImages != null) {
            this.galleryImages.clear();
        }
    }

    /**
     * ✅ NOMBRE d'images dans la galerie
     */
    public int getGalleryImagesCount() {
        return this.galleryImages != null ? this.galleryImages.size() : 0;
    }

    /**
     * ✅ VÉRIFIER si une image existe dans la galerie
     */
    public boolean hasGalleryImage(String imageUrl) {
        return this.galleryImages != null && this.galleryImages.contains(imageUrl);
    }

    /**
     * ✅ PREMIÈRE image de la galerie (pour vignette)
     */
    public String getFirstGalleryImage() {
        if (this.galleryImages != null && !this.galleryImages.isEmpty()) {
            return this.galleryImages.get(0);
        }
        return null;
    }
}