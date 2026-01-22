package com.hitendra.turf_booking_backend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.hitendra.turf_booking_backend.repository.UserRepository;
import com.hitendra.turf_booking_backend.repository.AdminProfileRepository;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.entity.Role;
import com.hitendra.turf_booking_backend.entity.AccountStatus;
import com.hitendra.turf_booking_backend.entity.AdminProfile;
import com.hitendra.turf_booking_backend.dto.user.CreateAdminRequest;
import com.hitendra.turf_booking_backend.dto.user.AdminProfileDto;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminProfileService {

    private final AdminProfileRepository adminProfileRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    // ---------------------------------------------------------
    // Create Admin
    // ---------------------------------------------------------
    public AdminProfileDto createAdmin(CreateAdminRequest request) {

        // Check if phone exists
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new RuntimeException("User with this phone number already exists");
        }

        // Create base User (single role = ADMIN)
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        user = userRepository.save(user);

        // Create Admin Profile
        AdminProfile adminProfile = AdminProfile.builder()
                .user(user)
                .city(request.getCity())
                .businessName(request.getBusinessName())
                .businessAddress(request.getBusinessAddress())
                .gstNumber(request.getGstNumber())
                .build();

        adminProfile = adminProfileRepository.save(adminProfile);

        return mapToDto(adminProfile);
    }

    // ---------------------------------------------------------
    // Get Admin by AdminProfile ID
    // ---------------------------------------------------------
    public AdminProfileDto getAdminById(Long id) {
        AdminProfile adminProfile = adminProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        return mapToDto(adminProfile);
    }

    // ---------------------------------------------------------
    // Get Admin by User ID
    // ---------------------------------------------------------
    public AdminProfileDto getAdminByUserId(Long userId) {
        AdminProfile adminProfile = adminProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found"));
        return mapToDto(adminProfile);
    }

    // ---------------------------------------------------------
    // Get all Admins
    // ---------------------------------------------------------
    public PaginatedResponse<AdminProfileDto> getAllAdmins(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AdminProfile> adminPage = adminProfileRepository.findAll(pageable);

        List<AdminProfileDto> content = adminPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                adminPage.getNumber(),
                adminPage.getSize(),
                adminPage.getTotalElements(),
                adminPage.getTotalPages(),
                adminPage.isLast()
        );
    }

    // ---------------------------------------------------------
    // Delete Admin (Manager Only)
    // ---------------------------------------------------------
    public void deleteAdmin(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        // Delete user → cascade deletes admin profile
        userRepository.delete(user);
    }

    // ---------------------------------------------------------
    // GDPR/Privacy Policy Compliant Account Deletion
    // ---------------------------------------------------------
    /**
     * GDPR/Privacy Policy Compliant Admin Account Deletion
     *
     * This method implements a policy-compliant account deletion that:
     * 1. PERMANENTLY DELETES all personally identifiable information (PII)
     * 2. UNLINKS all bookings created by this admin (sets adminProfile = NULL)
     * 3. PRESERVES booking/transaction records in anonymized form for business records
     * 4. PRESERVES services but removes admin PII from them
     * 5. ENSURES the deleted admin cannot be re-identified
     * 6. Is SAFE for Play Store and App Store review
     *
     * Data REMOVED:
     * - Name, Email, Phone number
     * - OAuth identifiers (Google ID, Apple ID)
     * - Business name, address, GST number, city
     *
     * Data PRESERVED (anonymized):
     * - Booking records with adminProfile = NULL
     * - Services (for existing bookings to remain valid)
     *
     * @param userId The user ID of the admin requesting deletion
     */
    public void permanentlyDeleteAdminAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("User is not an admin");
        }

        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new RuntimeException("Account is already deleted");
        }

        AdminProfile adminProfile = adminProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found"));

        log.info("ADMIN ACCOUNT DELETION INITIATED - User ID: {}, Name: {}, Email: {}, Business: {}",
                userId, user.getName(), user.getEmail(), adminProfile.getBusinessName());

        // STEP 1: Unlink ALL bookings created by this admin (preserve booking records, remove admin reference)
        int unlinkedBookings = bookingRepository.unlinkBookingsFromAdmin(adminProfile.getId());
        log.info("Unlinked {} admin-created bookings from admin profile {}", unlinkedBookings, adminProfile.getId());

        // STEP 2: Clear PII from AdminProfile (keep record for service references)
        adminProfile.setBusinessName(null);
        adminProfile.setBusinessAddress(null);
        adminProfile.setGstNumber(null);
        adminProfile.setCity(null);
        adminProfileRepository.save(adminProfile);

        // STEP 3: Permanently remove ALL PII from User record
        String deletedPlaceholder = "DELETED_" + UUID.randomUUID().toString();

        // Remove all personally identifiable information
        user.setName(null);
        user.setEmail(deletedPlaceholder + "@deleted.invalid");
        user.setPhone(null);

        // Remove OAuth identifiers
        user.setOauthProvider(null);
        user.setOauthProviderId(null);

        // Mark account as deleted and disabled
        user.setAccountStatus(AccountStatus.DELETED);
        user.setEnabled(false);
        user.setDeletedAt(Instant.now());
        user.setEmailVerified(false);

        userRepository.save(user);

        log.info("ADMIN ACCOUNT DELETION COMPLETED - User ID: {}, Admin Profile ID: {} - All PII permanently removed, {} bookings preserved (anonymized)",
                userId, adminProfile.getId(), unlinkedBookings);
    }

    // ---------------------------------------------------------
    // Map AdminProfile → DTO
    // ---------------------------------------------------------
    private AdminProfileDto mapToDto(AdminProfile adminProfile) {

        User user = adminProfile.getUser();

        return AdminProfileDto.builder()
                .id(adminProfile.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .city(adminProfile.getCity())
                .businessName(adminProfile.getBusinessName())
                .businessAddress(adminProfile.getBusinessAddress())
                .gstNumber(adminProfile.getGstNumber())
                .build();
    }
}
