package com.store.store.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "{validation.required}")
        @Email(message = "{validation.email}")
        @Size(max = 100, message = "{validation.size.max}")
        String username,

        @NotBlank(message = "{validation.required}")
        @Size(min = 8, max = 128, message = "{validation.login.password.size}")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
                message = "{validation.login.password.pattern}"
        )
        String password



) {
}