package com.store.store.dto;

public record ContactResponseDto(Long contactId, String name, String email,
                                 String mobileNumber, String message, String status) {
}