package com.store.store.service;

import com.store.store.dto.auth.LoginRequestDto;
import com.store.store.dto.auth.LoginResponseDto;
import com.store.store.dto.auth.RegisterRequestDto;


public interface IAuthService {


    void registerUser(RegisterRequestDto request);
    LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent);
}