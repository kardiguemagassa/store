package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.auth.LoggingResponseDto;

import com.store.store.service.impl.MessageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-01
 *
 * @see org.slf4j.Logger
 * @see ch.qos.logback.classic.Logger
 */
@Tag(name = "Logging", description = "API de test des niveaux de logging")
@RestController
@RequestMapping("/api/v1/logging")
@RequiredArgsConstructor
@Slf4j
public class LoggingController {

    private final MessageServiceImpl messageService;

    @Operation(
            summary = "Tester les niveaux de logging",
            description = "Génère des messages de log pour tous les niveaux (TRACE, DEBUG, INFO, WARN, ERROR) " +
                    "afin de vérifier la configuration du système de logging"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Test de logging effectué avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<LoggingResponseDto>> testLogging() {
        log.info("GET /api/v1/logging - Starting logging test");

        // GÉNÉRATION DES LOGS DE TEST

        log.trace("🔍 TRACE: This is a very detailed trace log. Used for tracking execution flow.");
        log.debug("🐞 DEBUG: This is a debug message. Used for debugging.");
        log.info("ℹ️ INFO: This is an informational message. Application events.");
        log.warn("⚠️ WARN: This is a warning! Something might go wrong.");
        log.error("🚨 ERROR: An error occurred! This needs immediate attention.");

        log.info("Logging test completed successfully");

        // CONSTRUCTION DE LA RÉPONSE
        // Données de réponse
        LoggingResponseDto loggingData = new LoggingResponseDto("Logging tested successfully", "SUCCESS");

        // Message de succès localisé
        String successMessage = messageService.getMessage("api.success.logging.test.completed");

        // Wrapper dans ApiResponse
        ApiResponse<LoggingResponseDto> response = ApiResponse.success(successMessage, loggingData).withPath("/api/v1/logging");

        return ResponseEntity.ok(response);
    }
}