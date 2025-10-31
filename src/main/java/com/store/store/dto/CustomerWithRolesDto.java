package com.store.store.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class CustomerWithRolesDto {
    private Long customerId;
    private String name;
    private String email;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private Boolean isActive;
}