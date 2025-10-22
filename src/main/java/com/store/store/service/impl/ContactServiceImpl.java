package com.store.store.service.impl;

import com.store.store.constants.ApplicationConstants;
import com.store.store.dto.ContactRequestDto;
import com.store.store.dto.ContactResponseDto;
import com.store.store.entity.Contact;
import com.store.store.exception.BusinessException;
import com.store.store.exception.ContactNotFoundException;
import com.store.store.exception.ExceptionFactory;
import com.store.store.repository.ContactRepository;
import com.store.store.service.IContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements IContactService {

    private final ContactRepository contactRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

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
            // recupere le message definie dans message.properties
            throw exceptionFactory.businessError(getLocalizedMessage("error.contact.save.failed")
            );

        } catch (Exception e) {
            log.error("Unexpected error while saving contact message from: {}", contactRequestDto.getEmail(), e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.contact.save")
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactResponseDto> getAllOpenMessages() {
        try {
            log.info("Fetching all open contact messages");

            List<Contact> contacts = contactRepository.fetchByStatus(ApplicationConstants.OPEN_MESSAGE);

            log.info("Found {} open contact messages", contacts.size());
            return contacts.stream()
                    .map(this::mapToContactResponseDTO)
                    .collect(Collectors.toList());

        } catch (DataAccessException e) {
            log.error("Database error while fetching open contact messages", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.contact.fetch.failed")
            );

        } catch (Exception e) {
            log.error("Unexpected error while fetching open contact messages", e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.contact.fetch")
            );
        }
    }

    @Override
    @Transactional
    public void updateMessageStatus(Long contactId, String status) {
        try {
            log.info("Updating status for contact ID: {} to: {}", contactId, status);

            // Validation des paramètres
            validateUpdateParameters(contactId, status);

            Contact contact = contactRepository.findById(contactId)
                    .orElseThrow(() -> exceptionFactory.contactNotFound(contactId));

            // Validation métier : vérifier que le statut est valide
            validateStatusTransition(contact.getStatus(), status);

            contact.setStatus(status);
            contactRepository.save(contact);

            log.info("Contact ID: {} status successfully updated to: {}", contactId, status);

        } catch (ContactNotFoundException e) {
            // On relance telle quelle - c'est une exception métier spécifique
            throw e;

        } catch (DataAccessException e) {
            log.error("Database error while updating contact ID: {} status to: {}", contactId, status, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.contact.update.failed")
            );

        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error while updating contact ID: {} status to: {}", contactId, status, e);
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.unexpected.contact.update")
            );
        }
    }

    // MÉTHODES DE VALIDATION MÉTIER
    private void validateUpdateParameters(Long contactId, String status) {
        if (contactId == null || contactId <= 0) {
            throw exceptionFactory.validationError("contactId",
                    getLocalizedMessage("validation.contact.id.invalid"));
        }

        if (status == null || status.trim().isEmpty()) {
            throw exceptionFactory.validationError("status",
                    getLocalizedMessage("validation.contact.status.required"));
        }

        if (!isValidStatus(status)) {
            throw exceptionFactory.validationError("status",
                    getLocalizedMessage("validation.contact.status.invalid", status));
        }
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (ApplicationConstants.CLOSED_MESSAGE.equals(currentStatus) &&
                ApplicationConstants.OPEN_MESSAGE.equals(newStatus)) {
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.contact.cannot.reopen")
            );
        }

        if (ApplicationConstants.CLOSED_MESSAGE.equals(currentStatus) &&
                ApplicationConstants.CLOSED_MESSAGE.equals(newStatus)) {
            throw exceptionFactory.businessError(
                    getLocalizedMessage("error.contact.already.closed")
            );
        }
    }

    private boolean isValidStatus(String status) {
        return ApplicationConstants.OPEN_MESSAGE.equals(status) ||
                ApplicationConstants.CLOSED_MESSAGE.equals(status) ||
                ApplicationConstants.IN_PROGRESS_MESSAGE.equals(status);
    }

    // METHODES DE MAPPING
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
        contact.setStatus(ApplicationConstants.OPEN_MESSAGE);
        return contact;
    }

    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}