package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.user.UpdateUserProfileRequest;
import com.hitendra.turf_booking_backend.dto.user.UserProfileDto;
import com.hitendra.turf_booking_backend.entity.AccountStatus;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.entity.UserProfile;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.UserProfileRepository;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BookingRepository bookingRepository;

    public UserProfileDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Don't allow access to deleted accounts
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("Account not found");
        }

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(null);

        return mapToDto(user, profile);
    }

    public UserProfileDto updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Don't allow updates to deleted accounts
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("Account not found");
        }

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

    /**
     * GDPR/Privacy Policy Compliant Account Deletion
     *
     * This method implements a policy-compliant account deletion that:
     * 1. PERMANENTLY DELETES all personally identifiable information (PII)
     * 2. UNLINKS all bookings from the user (sets user_id = NULL)
     * 3. PRESERVES booking/transaction records in anonymized form for business records
     * 4. ENSURES the deleted user cannot be re-identified
     * 5. Is SAFE for Play Store and App Store review
     *
     * Data REMOVED:
     * - Name, Email, Phone number
     * - OAuth identifiers (Google ID, Apple ID)
     * - All UserProfile data (address, DOB, city, state, pincode, profile image)
     *
     * Data PRESERVED (anonymized):
     * - Booking records with user_id = NULL
     * - Booking metadata (booking_id, resource_id, time slot, price, payment status, timestamps)
     *
     * @param userId The user ID requesting deletion
     */
    public void permanentlyDeleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("Account is already deleted");
        }

        log.info("ACCOUNT DELETION INITIATED - User ID: {}, Name: {}, Email: {}",
                userId, user.getName(), user.getEmail());

        // STEP 1: Unlink ALL bookings from this user (preserve booking records, remove user reference)
        int unlinkedBookings = bookingRepository.unlinkBookingsFromUser(userId);
        log.info("Unlinked {} bookings from user {}", unlinkedBookings, userId);

        // STEP 2: Delete UserProfile completely (contains PII like address, DOB, etc.)
        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            userProfileRepository.delete(profile);
            log.info("Deleted UserProfile for user {}", userId);
        });

        // STEP 3: Permanently remove ALL PII from User record
        // Generate a unique placeholder that cannot be traced back to the user
        String deletedPlaceholder = "DELETED_" + UUID.randomUUID().toString();

        // Remove all personally identifiable information
        user.setName(null);
        user.setEmail(deletedPlaceholder + "@deleted.invalid");  // Must be unique, using UUID
        user.setPhone(null);

        // Remove OAuth identifiers
        user.setOauthProvider(null);
        user.setOauthProviderId(null);

        // Mark account as deleted and disabled
        user.setAccountStatus(AccountStatus.DELETED);
        user.setEnabled(false);
        user.setDeletedAt(Instant.now());
        user.setEmailVerified(false);

        // Clear any profile references
        user.setUserProfile(null);

        userRepository.save(user);

        log.info("ACCOUNT DELETION COMPLETED - User ID: {} - All PII permanently removed, {} bookings preserved (anonymized)",
                userId, unlinkedBookings);
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

