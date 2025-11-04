package com.store.store.service;

import com.store.store.dto.ProfileRequestDto;
import com.store.store.dto.ProfileResponseDto;
import com.store.store.entity.Customer;

/**
 * Service interface for handling profile-related operations.
 *
 * This interface provides methods to retrieve and update the profile
 * details of the authenticated customer, as well as to fetch the
 * customer record of the currently authenticated user.
 */
public interface IProfileService {

    ProfileResponseDto getProfile();
    ProfileResponseDto updateProfile(ProfileRequestDto profileRequestDto);
    Customer getAuthenticatedCustomer();
}