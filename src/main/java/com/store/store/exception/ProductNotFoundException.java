package com.store.store.exception;

public class ProductNotFoundException extends ResourceNotFoundException {
    public ProductNotFoundException(Long productId) {
        super("Produit", "id", productId.toString());
    }
}