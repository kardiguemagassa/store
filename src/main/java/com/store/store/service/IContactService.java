package com.store.store.service;

import com.store.store.dto.ContactRequestDto;

public interface IContactService {

    boolean saveContact(ContactRequestDto contactRequestDto);
}

