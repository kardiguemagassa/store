package com.store.store.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@Getter
public class CustomerWithRolesDto {
    private Long customerId;

    @NotBlank(message = "{validation.required}")
    @Size(min = 2, max = 100, message = "{validation.size.min.max}")
    private String name;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.email}")
    @Size(max = 150, message = "{validation.size.max}")
    private String email;

    @NotNull(message = "{validation.required}")
    private Set<String> roles;

    private Instant createdAt;

    @NotNull(message = "{validation.required}")
    private Boolean isActive;
}