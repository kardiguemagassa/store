package com.store.store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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


    // HELPER METHODS
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    public String getCategoryCode() {
        return category != null ? category.getCode() : null;
    }

    /**
     * Adds an image to the product's gallery.
     * If the gallery has not been initialized, it initializes a new gallery list.
     *
     * @param imageUrl the URL of the image to be added to the product's gallery
     */
    public void addGalleryImage(String imageUrl) {
        if (this.galleryImages == null) {
            this.galleryImages = new ArrayList<>();
        }
        this.galleryImages.add(imageUrl);
    }

    /**
     * Adds an image to the product's gallery at the specified position.
     * If the gallery has not been initialized, it initializes a new gallery list.
     * If the position is invalid (i.e., outside the bounds of the existing list), the image is added at the end of the list.
     *
     * @param imageUrl the URL of the image to be added to the product's gallery
     * @param position the position at which the image should be inserted in the gallery
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
     * Removes an image from the product's gallery based on the given URL.
     * If the gallery is not initialized, the method returns false.
     *
     * @param imageUrl the URL of the image to be removed from the gallery
     * @return true if the image was successfully removed, false otherwise
     */
    public boolean removeGalleryImage(String imageUrl) {
        if (this.galleryImages != null) {
            return this.galleryImages.remove(imageUrl);
        }
        return false;
    }

    /**
     * Removes an image from the product's gallery based on the specified position.
     * If the gallery is not initialized or the position is invalid (i.e., out of bounds),
     * the method returns null.
     *
     * @param position the position of the image to be removed from the gallery
     * @return the URL of the removed image if successful, or null if the gallery
     *         is uninitialized or the position is invalid
     */
    public String removeGalleryImage(int position) {
        if (this.galleryImages != null && position >= 0 && position < this.galleryImages.size()) {
            return this.galleryImages.remove(position);
        }
        return null;
    }

    /**
     * Moves an image within the product's gallery from one position to another.
     * If the gallery is not initialized or if the specified positions are invalid,
     * the method returns false.
     *
     * @param fromPosition the current position of the image in the gallery
     * @param toPosition the new position to which the image should be moved
     * @return true if the image was successfully moved, false otherwise
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
     * Clears all images from the product's gallery.
     * If the gallery has not been initialized, the method does nothing.
     */
    public void clearGalleryImages() {
        if (this.galleryImages != null) {
            this.galleryImages.clear();
        }
    }

    /**
     * Retrieves the count of images in the product's gallery.
     * If the gallery is uninitialized, the method returns 0.
     *
     * @return the number of images in the gallery, or 0 if the gallery is not initialized
     */
    public int getGalleryImagesCount() {
        return this.galleryImages != null ? this.galleryImages.size() : 0;
    }

    /**
     * Checks if the specified image URL exists in the product's gallery.
     *
     * @param imageUrl the URL of the image to check within the gallery
     * @return true if the image URL is present in the gallery, false otherwise
     */
    public boolean hasGalleryImage(String imageUrl) {
        return this.galleryImages != null && this.galleryImages.contains(imageUrl);
    }

    /**
     * Retrieves the first image URL from the product's gallery.
     * If the gallery is not initialized or contains no images, the method returns null.
     *
     * @return the URL of the first image in the gallery, or null if the gallery is uninitialized or empty
     */
    public String getFirstGalleryImage() {
        if (this.galleryImages != null && !this.galleryImages.isEmpty()) {
            return this.galleryImages.get(0);
        }
        return null;
    }
}