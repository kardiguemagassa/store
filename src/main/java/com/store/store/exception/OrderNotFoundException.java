package com.store.store.exception;

public class OrderNotFoundException extends ResourceNotFoundException {
    public OrderNotFoundException(Long orderId) {
        super("Commande", "id", orderId.toString());
    }
}