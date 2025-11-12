package com.store.store.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record LogoutResponse(
        String message,
        boolean revoked,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
        LocalDateTime logoutTime
) {
    public static LogoutResponse success() {
        return new LogoutResponse("Déconnexion réussie", true, LocalDateTime.now());
    }
}
