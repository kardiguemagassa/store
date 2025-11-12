package com.store.store.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Represents a data transfer object (DTO) used for logout requests.
 * This record encapsulates the necessary data required to perform a logout operation.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-11-01
 */
public record LogoutRequestDto(

        @NotBlank(message = "{validation.required}")
        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$",
                message = "{validation.logout.refreshToken.pattern}")
        String refreshToken
) {
}
