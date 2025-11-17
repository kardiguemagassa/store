package com.store.store.service;

import com.store.store.dto.contact.ContactRequestDto;
import com.store.store.dto.contact.ContactResponseDto;

import java.util.List;

/**
 * @author Kardigué
 * @version 1.0
 * @since 2025-10-01
 */
public interface IContactService {

    boolean saveContact(ContactRequestDto contactRequestDto);

    List<ContactResponseDto> getAllOpenMessages();

    void updateMessageStatus(Long contactId, String status);
}

