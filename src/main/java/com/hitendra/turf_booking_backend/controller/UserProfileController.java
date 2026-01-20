package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.user.UpdateUserProfileRequest;
import com.hitendra.turf_booking_backend.dto.user.UserProfileDto;
import com.hitendra.turf_booking_backend.security.service.UserDetailsImplementation;
import com.hitendra.turf_booking_backend.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "User Profile", description = "User profile APIs")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    @Operation(
        summary = "Get user profile",
        description = "Get complete user profile including basic info"
    )
    public ResponseEntity<UserProfileDto> getUserProfile() {
        Long userId = getCurrentUserId();
        UserProfileDto profile = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(
        summary = "Update user profile",
        description = "Update user profile information"
    )
    public ResponseEntity<UserProfileDto> updateUserProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        Long userId = getCurrentUserId();
        UserProfileDto profile = userProfileService.updateUserProfile(userId, request);
        return ResponseEntity.ok(profile);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImplementation userDetails = (UserDetailsImplementation) authentication.getPrincipal();
        return userDetails.getId();
    }
}

