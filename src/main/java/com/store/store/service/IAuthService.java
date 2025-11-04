package com.store.store.service;

import com.store.store.dto.LoginRequestDto;
import com.store.store.dto.LoginResponseDto;
import com.store.store.dto.RegisterRequestDto;

/**
 * Service interface for managing authentication operations such as user registration
 * and login functionality.
 *
 * @author Kardigué
 * @version 3.0 (JWT + Cookies)
 * @since 2025-11-01
 */
public interface IAuthService {


    void registerUser(RegisterRequestDto request);
    LoginResponseDto login(LoginRequestDto request, String ipAddress, String userAgent);
}