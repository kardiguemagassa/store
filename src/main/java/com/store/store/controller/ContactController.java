package com.store.store.controller;

import com.store.store.dto.common.ApiResponse;
import com.store.store.dto.contact.ContactInfoDto;
import com.store.store.dto.contact.ContactRequestDto;
import com.store.store.dto.contact.ContactResponseDto;
import com.store.store.service.IContactService;

import com.store.store.service.impl.MessageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author Kardigué
 * @version 2.0
 * @since 2025-01-01
 *
 * @see IContactService
 * @see ContactRequestDto
 * @see ContactResponseDto
 * @see ApiResponse
 */
@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "Contact", description = "API de gestion des messages de contact")
public class ContactController {

    private final IContactService contactService;
    private final ContactInfoDto contactInfoDto;
    private final MessageServiceImpl messageService;


    // ENDPOINTS PUBLICS
    @Operation(
            summary = "Envoyer un message de contact",
            description = "Permet aux visiteurs d'envoyer un message via le formulaire de contact. " +
                    "Les champs sont validés automatiquement."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Message envoyé avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Erreur de validation des données",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveContact(
            @Valid @RequestBody ContactRequestDto contactRequestDto) {

        contactService.saveContact(contactRequestDto);

        // Message localisé via MessageService
        String message = messageService.getMessage("api.success.contact.created");

        ApiResponse<Void> response = ApiResponse.<Void>created(message, null).withPath("/api/v1/contacts");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(
            summary = "Obtenir les informations de contact",
            description = "Récupère les coordonnées de l'entreprise (téléphone, email, adresse) " +
                    "configurées dans les propriétés de l'application."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Informations de contact récupérées",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ContactInfoDto.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<ContactInfoDto> getContactInfo() {
        return ResponseEntity.ok(contactInfoDto);
    }

    // ENDPOINTS ADMIN (Nécessitent ROLE_ADMIN)
    @Operation(
            summary = "Lister les messages de contact OPEN",
            description = "Récupère tous les messages de contact non traités (statut OPEN). " +
                    "Réservé aux administrateurs."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des messages récupérée",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé - Rôle ADMIN requis",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ContactResponseDto>>> getAllOpenMessages() {
        List<ContactResponseDto> messages = contactService.getAllOpenMessages();

        String message = messageService.getMessage("api.success.list.retrieved", "messages de contact");

        ApiResponse<List<ContactResponseDto>> response = ApiResponse.success(message, messages)
                .withPath("/api/v1/contacts/messages");

        return ResponseEntity.ok(response);
    }


    @Operation(
            summary = "Mettre à jour le statut d'un message",
            description = "Change le statut d'un message de contact (OPEN → IN_PROGRESS → CLOSED). " +
                    "Réservé aux administrateurs. La réouverture de messages fermés est interdite."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Statut mis à jour avec succès",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Transition de statut invalide",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Accès refusé - Rôle ADMIN requis",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Message de contact non trouvé",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PatchMapping("/{contactId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateMessageStatus(
            @PathVariable Long contactId,
            @RequestParam String status) {

        contactService.updateMessageStatus(contactId, status);

        String message = messageService.getMessage("api.success.contact.status.updated");

        ApiResponse<Void> response = ApiResponse.<Void>success(message)
                .withPath("/api/v1/contacts/" + contactId + "/status");

        return ResponseEntity.ok(response);
    }
}