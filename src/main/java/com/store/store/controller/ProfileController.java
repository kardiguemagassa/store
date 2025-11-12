package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.profile.ProfileRequestDto;
import com.store.store.dto.profile.ProfileResponseDto;
import com.store.store.service.IProfileService;

import com.store.store.service.impl.MessageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready avec ApiResponse
 * @since 2025-01-06
 */
@Tag(name = "Profile", description = "API de gestion du profil utilisateur")
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final IProfileService profileService;
    private final MessageServiceImpl messageService;

    // CONSULTATION DU PROFIL
    @Operation(
            summary = "Obtenir le profil de l'utilisateur authentifié",
            description = "Retourne les informations complètes du profil incluant nom, email, téléphone et adresse"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profil récupéré avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié (JWT manquant ou invalide)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Profil utilisateur non trouvé"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponseDto>> getProfile() {
        log.info("GET /api/v1/profile - Fetching profile for authenticated user");

        // Appel au service
        ProfileResponseDto profileData = profileService.getProfile();

        log.info("Profile retrieved successfully for user: {}", profileData.getEmail());

        // Message de succès localisé
        String successMessage = messageService.getMessage("api.success.profile.retrieved");

        ApiResponse<ProfileResponseDto> response = ApiResponse.success(successMessage, profileData)
                .withPath("/api/v1/profile");

        return ResponseEntity.ok(response);
    }

    // MISE À JOUR DU PROFIL
    @Operation(
            summary = "Mettre à jour le profil utilisateur",
            description = "Modifie les informations personnelles et l'adresse de l'utilisateur authentifié"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profil mis à jour avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Données invalides (email déjà utilisé, validation échouée)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié (JWT manquant ou invalide)"
            )
    })
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponseDto>> updateProfile(
            @Validated @RequestBody ProfileRequestDto profileRequestDto) {

        log.info("PUT /api/v1/profile - Updating profile for authenticated user");

        // Appel au service
        ProfileResponseDto updatedProfile = profileService.updateProfile(profileRequestDto);

        log.info("Profile updated successfully for user: {} (emailUpdated: {})",
                updatedProfile.getEmail(), updatedProfile.isEmailUpdated());

        // Message de succès localisé (différent si email changé)
        String successMessage;
        if (updatedProfile.isEmailUpdated()) {
            successMessage = messageService.getMessage("api.success.profile.updated.email.changed");
        } else {
            successMessage = messageService.getMessage("api.success.profile.updated");
        }

        ApiResponse<ProfileResponseDto> response = ApiResponse.success(successMessage, updatedProfile)
                .withPath("/api/v1/profile");

        return ResponseEntity.ok(response);
    }

    // TODO: Future endpoints
    // POST /api/v1/profile/avatar - Upload photo de profil
    // DELETE /api/v1/profile/avatar - Supprimer photo de profil
    // PATCH /api/v1/profile/password - Changer le mot de passe
}