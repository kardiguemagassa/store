package com.store.store.controller;

import com.store.store.dto.ProfileRequestDto;
import com.store.store.dto.ProfileResponseDto;
import com.store.store.service.IProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * ProfileController is a REST controller that handles HTTP requests
 * related to the user profile. It provides endpoints for retrieving
 * and updating a profile.
 *
 * The controller interacts with the IProfileService interface to perform
 * profile-related operations such as retrieving profile details and
 * updating the profile.
 *
 * All endpoints map to the `/api/v1/profile` URI.
 *
 * @author Kardigué
 * @version 3.0
 * @since 2025-11-01
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final IProfileService iProfileService;

    // ADD PROFILE IMAGE IMPLEMENTATION

    /**
     * Retrieves the profile details for the current user.
     *
     * This method invokes the IProfileService to obtain the user's profile
     * information and returns it as a response.
     *
     * @return ResponseEntity containing the user's profile details wrapped in a
     *         ProfileResponseDto object.
     */
    @GetMapping
    public ResponseEntity<ProfileResponseDto> getProfile() {
        ProfileResponseDto responseDto = iProfileService.getProfile();
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Updates the user profile based on the provided profile details.
     *
     * This method accepts a ProfileRequestDto object containing the updated profile
     * information, processes it by invoking the updateProfile method of the
     * IProfileService interface, and returns the updated profile details wrapped in
     * a ProfileResponseDto object.
     *
     * @param profileRequestDto the ProfileRequestDto object containing the updated
     *                          user profile details. This includes name, email, mobile
     *                          number, and optionally address information.
     * @return ResponseEntity containing the updated profile details wrapped in a
     *         ProfileResponseDto object.
     */
    @PutMapping
    public ResponseEntity<ProfileResponseDto> updateProfile(
            @Validated @RequestBody ProfileRequestDto profileRequestDto) {
        ProfileResponseDto responseDto = iProfileService.updateProfile(profileRequestDto);
        return ResponseEntity.ok(responseDto);
    }
}
