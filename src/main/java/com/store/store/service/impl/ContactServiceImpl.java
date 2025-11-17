package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.contact.ContactRequestDto;
import com.store.store.dto.contact.ContactResponseDto;
import com.store.store.entity.Contact;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ExceptionFactory;

import com.store.store.repository.ContactRepository;
import com.store.store.service.IContactService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Kardigué
 * @version 2.0
 * @since 2025-01-01
 *
 * @see IContactService
 * @see ContactRequestDto
 * @see ContactResponseDto
 * @see ExceptionFactory
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements IContactService {

    private final ContactRepository contactRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageServiceImpl messageService;

    // OPÉRATIONS CRUD

    @Override
    @Transactional
    public boolean saveContact(ContactRequestDto contactRequestDto) {
        try {
            log.info("Saving contact message from: {}", contactRequestDto.getEmail());

            Contact contact = transformToEntity(contactRequestDto);
            Contact savedContact = contactRepository.save(contact);

            log.info("Contact message saved successfully with ID: {}", savedContact.getContactId());
            return true;

        } catch (DataAccessException e) {
            log.error("Database error while saving contact message from: {}", contactRequestDto.getEmail(), e);

            // Utilisation de MessageService pour récupérer le message localisé
            throw exceptionFactory.businessError(messageService.getMessage("error.contact.save.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while saving contact message from: {}", contactRequestDto.getEmail(), e);

            throw exceptionFactory.businessError(messageService.getMessage("api.error.internal"));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponseDto> getAllOpenMessages() {
        try {
            log.info("Fetching all open contact messages");
            List<Contact> contacts = contactRepository.fetchByStatus(ApplicationConstants.OPEN_MESSAGE);
            log.info("Found {} open contact messages", contacts.size());
            return contacts.stream().map(this::mapToContactResponseDTO).collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching open contact messages", e);
            throw exceptionFactory.businessError(messageService.getMessage("error.contact.fetch.failed"));

        } catch (Exception e) {
            log.error("Unexpected error while fetching open contact messages", e);
            throw exceptionFactory.businessError(messageService.getMessage("api.error.internal"));
        }
    }

    @Override
    @Transactional
    public void updateMessageStatus(Long contactId, String status) {
        try {
            log.info("Updating status for contact ID: {} to: {}", contactId, status);

            // Validation des paramètres d'entrée
            validateUpdateParameters(contactId, status);

            // Récupération du contact existant
            Contact contact = contactRepository.findById(contactId).orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Contact", "id", contactId.toString()));

            // Validation de la transition de statut métier
            validateStatusTransition(contact.getStatus(), status);

            // Mise à jour et sauvegarde
            contact.setStatus(status);
            contactRepository.save(contact);

            log.info("Contact ID: {} status successfully updated to: {}", contactId, status);

        } catch (DataAccessException e) {
            log.error("Database error while updating contact ID: {} status to: {}", contactId, status, e);

            throw exceptionFactory.businessError(messageService.getMessage("error.contact.update.failed"));

        } catch (Exception e) {
            // Si c'est déjà une exception métier (BusinessException, ResourceNotFoundException),
            // on la relance telle quelle
            if (e instanceof BusinessException || e instanceof RuntimeException) {
                throw e;
            }

            log.error("Unexpected error while updating contact ID: {} status to: {}", contactId, status, e);

            throw exceptionFactory.businessError(messageService.getMessage("api.error.internal"));
        }
    }

    // MÉTHODES DE VALIDATION MÉTIER

    private void validateUpdateParameters(Long contactId, String status) {
        // Validation de l'ID
        if (contactId == null || contactId <= 0) {
            throw exceptionFactory.validationError("contactId", messageService.getMessage("validation.contact.id.invalid"));
        }
        // Validation du statut (non vide)
        if (status == null || status.trim().isEmpty()) {
            throw exceptionFactory.validationError("status", messageService.getMessage("validation.contact.status.required"));
        }

        // Validation du statut (valeur autorisée)
        if (!isValidStatus(status)) {
            throw exceptionFactory.validationError("status", messageService.getMessage("validation.contact.status.invalid", status));
        }
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Règle 1 : Impossible de rouvrir un message fermé
        if (ApplicationConstants.CLOSED_MESSAGE.equals(currentStatus) &&
                ApplicationConstants.OPEN_MESSAGE.equals(newStatus)) {
            throw exceptionFactory.businessError(messageService.getMessage("error.contact.cannot.reopen"));
        }

        // Règle 2 : Impossible de fermer un message déjà fermé
        if (ApplicationConstants.CLOSED_MESSAGE.equals(currentStatus) &&
                ApplicationConstants.CLOSED_MESSAGE.equals(newStatus)) {
            throw exceptionFactory.businessError(messageService.getMessage("error.contact.already.closed"));
        }
    }

    private boolean isValidStatus(String status) {
        return ApplicationConstants.OPEN_MESSAGE.equals(status) ||
                ApplicationConstants.CLOSED_MESSAGE.equals(status) ||
                ApplicationConstants.IN_PROGRESS_MESSAGE.equals(status);
    }

    // MÉTHODES DE MAPPING (Entity ↔ DTO)

    private ContactResponseDto mapToContactResponseDTO(Contact contact) {
        return new ContactResponseDto(
                contact.getContactId(),
                contact.getName(),
                contact.getEmail(),
                contact.getMobileNumber(),
                contact.getMessage(),
                contact.getStatus()
        );
    }

    private Contact transformToEntity(ContactRequestDto contactRequestDto) {
        Contact contact = new Contact();
        BeanUtils.copyProperties(contactRequestDto, contact);
        contact.setStatus(ApplicationConstants.OPEN_MESSAGE); // Statut initial par défaut
        return contact;
    }
}