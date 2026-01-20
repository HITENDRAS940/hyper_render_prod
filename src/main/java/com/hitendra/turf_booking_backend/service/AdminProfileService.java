package com.hitendra.turf_booking_backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.hitendra.turf_booking_backend.repository.UserRepository;
import com.hitendra.turf_booking_backend.repository.AdminProfileRepository;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.entity.Role;
import com.hitendra.turf_booking_backend.entity.AdminProfile;
import com.hitendra.turf_booking_backend.dto.user.CreateAdminRequest;
import com.hitendra.turf_booking_backend.dto.user.AdminProfileDto;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProfileService {

    private final AdminProfileRepository adminProfileRepository;
    private final UserRepository userRepository;

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
