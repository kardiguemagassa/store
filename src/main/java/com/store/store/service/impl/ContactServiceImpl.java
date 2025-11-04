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

/**
 * Implementation of the {@link IContactService} interface to handle operations
 * related to contact messages. This class provides functionality for saving
 * contact messages, retrieving open messages, and updating the status of a contact message.
 *
 * It interacts with the database through the {@link ContactRepository} and
 * handles exceptions using the {@link ExceptionFactory}.
 *
 * The business logic ensures:
 * - Validation of message fields and status updates.
 * - Transformation of data between DTOs and entity models.
 * - Localization for error and validation messages.
 *
 * This service is transactional and ensures data integrity during its operations.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactServiceImpl implements IContactService {

    private final ContactRepository contactRepository;
    private final ExceptionFactory exceptionFactory;
    private final MessageSource messageSource;

    /**
     * Saves a contact message represented by the given DTO into the database.
     * Logs the process and handles potential exceptions.
     *
     * @param contactRequestDto the contact request data transfer object containing details such as
     *                          name, email, mobile number, and message to be saved
     * @return true if the contact message is successfully saved, otherwise throws appropriate exceptions
     * @throws BusinessException if there is a business error during the saving process, with localized error messages
     */
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

    /**
     * Retrieves all contact messages with an open status from the database.
     * Logs the operation and handles any potential exceptions during the process.
     *
     * This method maps the retrieved Contact entities to their corresponding
     * ContactResponseDto objects.
     *
     * @return a list of ContactResponseDto objects representing all open contact messages
     * @throws BusinessException if there is a business or unexpected error during the retrieval process
     */
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

    /**
     * Updates the status of a contact message in the database for the given contact ID.
     * Uses various validation steps to ensure that the provided parameters and the
     * status transition are valid. Handles and logs potential exceptions that may arise
     * during the process.
     *
     * @param contactId the unique identifier of the contact whose status needs to be updated
     * @param status the new status to be set for the contact message
     * @throws ContactNotFoundException if the contact with the given ID is not found
     * @throws BusinessException if there is a validation or business logic error during status update
     * @throws DataAccessException if a database-related error occurs during the update process
     */
    @Override
    @Transactional
    public void updateMessageStatus(Long contactId, String status) {
        try {
            log.info("Updating status for contact ID: {} to: {}", contactId, status);

            // Validation des paramètres
            validateUpdateParameters(contactId, status);

            Contact contact = contactRepository.findById(contactId)
                    .orElseThrow(() -> exceptionFactory.resourceNotFound(
                            "Product", "id", contactId.toString()));

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

    /**
     * Validates the parameters provided for updating the status of a contact message.
     * Ensures that the contact ID is valid, the status is non-null and not empty,
     * and that the status provided is a valid status.
     *
     * @param contactId the unique identifier of the contact to be updated; must be non-null and greater than 0
     * @param status the new status to be applied; must be non-null, non-empty, and a valid status
     */
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

    /**
     * Validates the transition from the current status to the new status for a contact message.
     * Ensures that invalid or prohibited status transitions are flagged with appropriate errors.
     *
     * @param currentStatus the current status of the contact message; must be a valid status from ApplicationConstants
     * @param newStatus the new status to which the contact message is transitioning; must be a valid status from ApplicationConstants
     * @throws BusinessException if the transition from the current status to the new status is not allowed
     */
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

    /**
     * Checks whether the given status is valid. A valid status is one of the predefined
     * statuses specified in the {@code ApplicationConstants} class.
     *
     * @param status the status string to be validated; must not be null or empty
     * @return true if the provided status matches one of the valid statuses (OPEN_MESSAGE, CLOSED_MESSAGE, or IN_PROGRESS_MESSAGE),
     *         otherwise false
     */
    private boolean isValidStatus(String status) {
        return ApplicationConstants.OPEN_MESSAGE.equals(status) ||
                ApplicationConstants.CLOSED_MESSAGE.equals(status) ||
                ApplicationConstants.IN_PROGRESS_MESSAGE.equals(status);
    }

    // METHODES DE MAPPING

    /**
     * Maps the given Contact entity to a ContactResponseDto object.
     * Transforms the fields from the Contact entity to their corresponding fields
     * in the ContactResponseDto record.
     *
     * @param contact the Contact entity to be mapped, containing properties such as
     *                contactId, name, email, mobileNumber, message, and status
     * @return a ContactResponseDto object populated with the data from the provided
     *         Contact entity
     */
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

    /**
     * Transforms a ContactRequestDto object into a Contact entity.
     * Copies the properties from the DTO to the entity and sets
     * the status to a predefined value.
     *
     * @param contactRequestDto the data transfer object containing details
     *                          such as name, email, mobile number, and message.
     *                          Used for mapping to the Contact entity.
     * @return a Contact entity populated with the data from the given DTO
     *         and the default status value set.
     */
    private Contact transformToEntity(ContactRequestDto contactRequestDto) {
        Contact contact = new Contact();
        BeanUtils.copyProperties(contactRequestDto, contact);
        contact.setStatus(ApplicationConstants.OPEN_MESSAGE);
        return contact;
    }

    /**
     * Retrieves a localized message based on the given message code and argument values.
     *
     * This method utilizes the message source to resolve the provided message code into
     * a localized message string, using the current locale from the LocaleContextHolder.
     *
     * @param code the message code to be resolved into a localized message, must not be null or empty
     * @param args optional arguments to be inserted into the message, if applicable
     * @return the localized message corresponding to the given code and arguments
     */
    private String getLocalizedMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}