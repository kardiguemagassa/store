package com.store.store.controller;

import com.store.store.dto.LoggingResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LoggingController provides a REST API endpoint for testing various logging levels.
 *
 * This controller demonstrates the usage of five logging levels (TRACE, DEBUG, INFO, WARN, and ERROR)
 * by logging messages with each level. It serves as an example for monitoring and debugging in an application.
 *
 * The endpoint responds with a JSON payload that includes a success message and status.
 *
 * Endpoint:
 * - HTTP Method: GET
 * - Path: /api/v1/logging
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-10-01
 */
@RestController
@RequestMapping("/api/v1/logging")
@Slf4j
public class LoggingController {

    /**
     * Tests various logging levels by generating log messages for TRACE, DEBUG, INFO, WARN, and ERROR.
     *
     * This method demonstrates the use of different log levels in an application and returns a structured JSON response
     * indicating the result of the logging operation.
     *
     * @return ResponseEntity containing a LoggingResponseDto with a success message and status.
     */
    @GetMapping
    public ResponseEntity<LoggingResponseDto> testLogging() {
        log.trace("🔍 TRACE: This is a very detailed trace log. Used for tracking execution flow.");
        log.debug("🐞 DEBUG: This is a debug message. Used for debugging.");
        log.info("ℹ️ INFO: This is an informational message. Application events.");
        log.warn("⚠️ WARN: This is a warning! Something might go wrong.");
        log.error("🚨 ERROR: An error occurred! This needs immediate attention.");

        // Retourne un objet JSON structuré
        LoggingResponseDto response = new LoggingResponseDto("Logging tested successfully", "SUCCESS");
        return ResponseEntity.ok(response);
    }
}
