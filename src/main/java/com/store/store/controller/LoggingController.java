package com.store.store.controller;

import com.store.store.dto.LoggingResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logging")
@Slf4j
public class LoggingController {

    @GetMapping
    public ResponseEntity<LoggingResponseDto> testLogging() {
        log.trace("🔍 TRACE: This is a very detailed trace log. Used for tracking execution flow.");
        log.debug("🐞 DEBUG: This is a debug message. Used for debugging.");
        log.info("ℹ️ INFO: This is an informational message. Application events.");
        log.warn("⚠️ WARN: This is a warning! Something might go wrong.");
        log.error("🚨 ERROR: An error occurred! This needs immediate attention.");

        // Retourne un objet JSON structuré
        LoggingResponseDto response = new LoggingResponseDto(
                "Logging tested successfully",
                "SUCCESS"
        );
        return ResponseEntity.ok(response);
    }
}
