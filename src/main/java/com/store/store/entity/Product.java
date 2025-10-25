package com.store.store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Helper methods
    public String getCategoryName() {
        return category != null ? category.getName() : null;
    }

    public String getCategoryCode() {
        return category != null ? category.getCode() : null;
    }
}