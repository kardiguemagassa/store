package com.store.store.service;

import com.store.store.dto.ContactRequestDto;
import com.store.store.dto.ContactResponseDto;

import java.util.List;

/**
 * Interface for managing contact-related operations, including saving messages,
 * retrieving all open messages, and updating message statuses.
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
public interface IContactService {

    boolean saveContact(ContactRequestDto contactRequestDto);

    List<ContactResponseDto> getAllOpenMessages();

    void updateMessageStatus(Long contactId, String status);
}

