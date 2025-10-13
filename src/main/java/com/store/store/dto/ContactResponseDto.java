package com.store.store.dto;

import lombok.Builder;

@Builder
public record ContactResponseDto(Long contactId, String name, String email,
                                 String mobileNumber, String message, String status) {
}