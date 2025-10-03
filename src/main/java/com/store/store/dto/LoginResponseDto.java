package com.store.store.dto;

public record LoginResponseDto(String message, UserDto user, String jwtToken) {
}
