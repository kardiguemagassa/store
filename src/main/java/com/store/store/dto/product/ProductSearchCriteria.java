package com.store.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        @Size(max = 100, message = "{validation.size.max}")
        String searchQuery,

        @Size(max = 50, message = "{validation.size.max}")
        @Pattern(regexp = "^[A-Z0-9_]*$", message = "{validation.pattern}")
        String categoryCode,

        Boolean activeOnly,

        @DecimalMin(value = "0.0", message = "{validation.decimal.min}")
        BigDecimal minPrice,

        @DecimalMin(value = "0.0", message = "{validation.decimal.min}")
        BigDecimal maxPrice,

        Boolean inStockOnly,

        SortBy sortBy,

        SortDirection sortDirection
) {
    public ProductSearchCriteria {
        // Validation et valeurs par défaut
        if (activeOnly == null) activeOnly = true;
        if (sortBy == null) sortBy = SortBy.NAME;
        if (sortDirection == null) sortDirection = SortDirection.ASC;

        // Validation des prix
        validatePriceRange(minPrice, maxPrice);
    }

    // ENUMS
    public enum SortBy {
        NAME,
        PRICE,
        POPULARITY,
        CREATED_DATE,
        STOCK_QUANTITY
    }

    public enum SortDirection {
        ASC,
        DESC
    }

    // MÉTHODES DE VALIDATION
    /**
     * Valide la cohérence de la plage de prix
     */
    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Le prix minimum ne peut pas être supérieur au prix maximum");
        }
    }

    /**
     * Vérifie si une recherche par texte est active
     */
    public boolean hasSearchQuery() {
        return searchQuery != null && !searchQuery.trim().isEmpty();
    }

    /**
     * Vérifie si un filtre par catégorie est actif
     */
    public boolean hasCategoryFilter() {
        return categoryCode != null && !categoryCode.trim().isEmpty();
    }

    /**
     * Vérifie si un filtre par prix est actif
     */
    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }

    /**
     * Vérifie si un filtre de stock est actif
     */
    public boolean hasStockFilter() {
        return inStockOnly != null && inStockOnly;
    }

    /**
     * Vérifie si des filtres sont actifs
     */
    public boolean hasFilters() {
        return hasSearchQuery() || hasCategoryFilter() || hasPriceFilter() || hasStockFilter();
    }

    /**
     * Retourne le champ de tri sous forme de chaîne pour JPA
     */
    public String getSortField() {
        return switch (sortBy) {
            case NAME -> "name";
            case PRICE -> "price";
            case POPULARITY -> "popularity";
            case CREATED_DATE -> "createdAt";
            case STOCK_QUANTITY -> "stockQuantity";
        };
    }

    /**
     * Retourne la direction de tri sous forme de chaîne pour JPA
     */
    public String getSortDirection() {
        return sortDirection.name().toLowerCase();
    }

    // FACTORY METHODS
    /**
     * Crée un critère de recherche simple par query
     */
    public static ProductSearchCriteria bySearchQuery(String searchQuery) {
        return builder().searchQuery(searchQuery).build();
    }

    /**
     * Crée un critère de recherche par catégorie
     */
    public static ProductSearchCriteria byCategory(String categoryCode) {
        return builder().categoryCode(categoryCode).build();
    }

    /**
     * Crée un critère de recherche par plage de prix
     */
    public static ProductSearchCriteria byPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return builder().minPrice(minPrice).maxPrice(maxPrice).build();
    }

    /**
     * Crée un critère pour les produits en stock
     */
    public static ProductSearchCriteria inStock() {
        return builder().inStockOnly(true).build();
    }

    /**
     * Crée un critère pour tous les produits (sans filtre)
     */
    public static ProductSearchCriteria all() {
        return builder().build();
    }

    // BUILDER
    public static ProductSearchCriteriaBuilder builder() {
        return new ProductSearchCriteriaBuilder();
    }

    public static class ProductSearchCriteriaBuilder {
        private String searchQuery;
        private String categoryCode;
        private Boolean activeOnly = true;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Boolean inStockOnly;
        private SortBy sortBy = SortBy.NAME;
        private SortDirection sortDirection = SortDirection.ASC;

        public ProductSearchCriteriaBuilder searchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
            return this;
        }

        public ProductSearchCriteriaBuilder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public ProductSearchCriteriaBuilder activeOnly(Boolean activeOnly) {
            this.activeOnly = activeOnly;
            return this;
        }

        public ProductSearchCriteriaBuilder minPrice(BigDecimal minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public ProductSearchCriteriaBuilder maxPrice(BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public ProductSearchCriteriaBuilder priceRange(BigDecimal minPrice, BigDecimal maxPrice) {
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            return this;
        }

        public ProductSearchCriteriaBuilder inStockOnly(Boolean inStockOnly) {
            this.inStockOnly = inStockOnly;
            return this;
        }

        public ProductSearchCriteriaBuilder sortBy(SortBy sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public ProductSearchCriteriaBuilder sortDirection(SortDirection sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        public ProductSearchCriteriaBuilder sortByPriceDesc() {
            this.sortBy = SortBy.PRICE;
            this.sortDirection = SortDirection.DESC;
            return this;
        }

        public ProductSearchCriteriaBuilder sortByPopularityDesc() {
            this.sortBy = SortBy.POPULARITY;
            this.sortDirection = SortDirection.DESC;
            return this;
        }

        public ProductSearchCriteriaBuilder sortByNewest() {
            this.sortBy = SortBy.CREATED_DATE;
            this.sortDirection = SortDirection.DESC;
            return this;
        }

        public ProductSearchCriteria build() {
            return new ProductSearchCriteria(
                    searchQuery, categoryCode, activeOnly, minPrice, maxPrice,
                    inStockOnly, sortBy, sortDirection
            );
        }
    }
}