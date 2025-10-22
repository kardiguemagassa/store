package com.store.store.exception;

public class ContactNotFoundException extends ResourceNotFoundException {
    public ContactNotFoundException(Long contactId) {
        super("Contact", "id", contactId.toString());
    }
}