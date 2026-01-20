package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.user.UpdateUserProfileRequest;
import com.hitendra.turf_booking_backend.dto.user.UserProfileDto;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.entity.UserProfile;
import com.hitendra.turf_booking_backend.repository.UserProfileRepository;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(null);

        return mapToDto(user, profile);
    }

    public UserProfileDto updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update user basic info
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        userRepository.save(user);

        // Update or create profile
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(UserProfile.builder().user(user).build());

        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getCity() != null) {
            profile.setCity(request.getCity());
        }
        if (request.getState() != null) {
            profile.setState(request.getState());
        }
        if (request.getPincode() != null) {
            profile.setPincode(request.getPincode());
        }

        profile = userProfileRepository.save(profile);

        return mapToDto(user, profile);
    }

    private UserProfileDto mapToDto(User user, UserProfile profile) {
        UserProfileDto.UserProfileDtoBuilder builder = UserProfileDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone());

        if (profile != null) {
            builder.id(profile.getId())
                    .address(profile.getAddress())
                    .dateOfBirth(profile.getDateOfBirth())
                    .city(profile.getCity())
                    .state(profile.getState())
                    .pincode(profile.getPincode())
                    .profileImageUrl(profile.getProfileImageUrl());
        }

        return builder.build();
    }
}

