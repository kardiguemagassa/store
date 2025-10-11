package com.store.store.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, String fieldValue) {
        super(String.format("%s introuvable avec les données %s : '%s'", resourceName, fieldName, fieldValue));
    }
}