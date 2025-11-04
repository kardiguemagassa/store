package com.store.store.controller;

import com.store.store.dto.ContactInfoDto;
import com.store.store.dto.ContactRequestDto;
import com.store.store.dto.SuccessResponseDto;
import com.store.store.service.IContactService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing contacts.
 * This controller provides endpoints for creating contact entries and retrieving contact information.
 * It interacts with the service layer to handle business logic.
 *
 *  @author Kardigué
 *  * @version 1.0
 *  * @since 2025-10-01
 */
@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final IContactService contactService;
    private final ContactInfoDto contactInfoDto;
    private final MessageSource messageSource;

    /**
     * Saves a new contact entry based on the provided contact details.
     * This method validates the input request body and invokes the service layer
     * to persist the contact information.
     *
     * @param contactRequestDto the DTO containing contact details to be saved.
     *                          It must be validated before processing.
     * @return a response entity containing a success message and HTTP status code indicating
     *         the creation of the new contact.
     */
    @Operation(summary = "Envoyer un message de contact")
    @PostMapping
    public ResponseEntity<SuccessResponseDto> saveContact(
            @Valid @RequestBody ContactRequestDto contactRequestDto) {

        contactService.saveContact(contactRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponseDto.builder()
                        //Retourne les infos de configuration dans properties
                        .message(getLocalizedMessage("success.contact.created"))
                        .status(HttpStatus.CREATED.value())
                        .build());
    }

    /**
     * Retrieves the contact information details such as phone number, email, and address.
     * The information is provided from the application properties through the ContactInfoDto object.
     *
     * @return a ResponseEntity containing the contact information data, encapsulated
     *         within a {@link ContactInfoDto} object and an HTTP 200 OK status.
     */
    @Operation(summary = "Obtenir les informations de contact")
    @GetMapping()
    public ResponseEntity<ContactInfoDto> getContactInfo() {
        return ResponseEntity.ok(contactInfoDto); // Retourne les infos de configuration
    }

    /**
     * Retrieves a localized message based on the provided message code and arguments.
     * This method resolves the message using the current locale held by the LocaleContextHolder.
     *
     * @param code the message code to look up, which acts as a key in the
     *             message source to retrieve the corresponding message.
     * @param args optional arguments to be substituted into the message placeholders,
     *             if applicable.
     * @return the localized message as a String, resolved using the specified code,
     *         arguments, and the current locale.
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}