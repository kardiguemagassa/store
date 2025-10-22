package com.store.store.controller;

import com.store.store.dto.ContactInfoDto;
import com.store.store.dto.ContactRequestDto;
import com.store.store.dto.SuccessResponseDto;
import com.store.store.service.IContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final IContactService contactService;
    private final ContactInfoDto contactInfoDto;
    private final MessageSource messageSource;

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

    @GetMapping()
    public ResponseEntity<ContactInfoDto> getContactInfo() {
        return ResponseEntity.ok(contactInfoDto); // Retourne les infos de configuration
    }

    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}