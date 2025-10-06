package com.store.store.service;

import com.store.store.dto.ProfileRequestDto;
import com.store.store.dto.ProfileResponseDto;

public interface IProfileService {

    ProfileResponseDto getProfile();
    ProfileResponseDto updateProfile(ProfileRequestDto profileRequestDto);
}