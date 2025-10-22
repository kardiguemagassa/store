package com.store.store.service;

import com.store.store.dto.ProfileRequestDto;
import com.store.store.dto.ProfileResponseDto;
import com.store.store.entity.Customer;

public interface IProfileService {

    ProfileResponseDto getProfile();
    ProfileResponseDto updateProfile(ProfileRequestDto profileRequestDto);
    Customer getAuthenticatedCustomer();
}