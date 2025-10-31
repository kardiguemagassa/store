package com.store.store.dto;

import java.math.BigDecimal;

public record ProductSearchCriteria(
        String searchQuery,
        String categoryCode,
        Boolean activeOnly,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStockOnly,
        SortBy sortBy,
        SortDirection sortDirection
) {
    public ProductSearchCriteria {
        // Valeurs par défaut
        if (activeOnly == null) activeOnly = true;
        if (sortBy == null) sortBy = SortBy.NAME;
        if (sortDirection == null) sortDirection = SortDirection.ASC;
    }

    public static ProductSearchCriteriaBuilder builder() {
        return new ProductSearchCriteriaBuilder();
    }

    public enum SortBy {
        NAME, PRICE, POPULARITY, CREATED_DATE
    }

    public enum SortDirection {
        ASC, DESC
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

        public ProductSearchCriteria build() {
            return new ProductSearchCriteria(
                    searchQuery, categoryCode, activeOnly, minPrice, maxPrice,
                    inStockOnly, sortBy, sortDirection
            );
        }
    }
}