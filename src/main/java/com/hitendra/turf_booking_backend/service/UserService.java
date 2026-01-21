package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.user.UpdateUserBasicInfoDto;
import com.hitendra.turf_booking_backend.dto.user.UserDto;
import com.hitendra.turf_booking_backend.dto.user.UserInfoDto;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.Role;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final AuthUtil authUtil;

    public String setNewUserName(UserDto userDto) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = ((UserDetails) authentication.getPrincipal()).getUsername();

        User user = userRepository.findUserByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("User Not found")
        );

        user.setName(userDto.getName());
        userRepository.save(user);

        return "User name updated successfully";
    }

    /**
     * Get all regular users (excluding admins and managers) with complete information
     */
    public PaginatedResponse<UserInfoDto> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> userPage = userRepository.findAllRegularUsers(pageable);

        List<UserInfoDto> content = userPage.getContent().stream()
                .map(this::convertToUserInfoDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    /**
     * Get user by ID with complete information
     */
    public UserInfoDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        if (user.getRole() != Role.USER) {
            throw new RuntimeException("User is not a regular user");
        }

        return convertToUserInfoDto(user);
    }

    private UserInfoDto convertToUserInfoDto(User user) {
        // Get booking statistics
        List<com.hitendra.turf_booking_backend.entity.Booking> userBookings =
                bookingRepository.findByUserId(user.getId());

        long totalBookings = userBookings.size();
        long confirmedBookings = userBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
        long cancelledBookings = userBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                .count();

        UserInfoDto dto = UserInfoDto.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .build();


        return dto;
    }

    public String setNewUserPhone(@Valid String phone) {
        User user = userRepository.findUserByPhone(phone);

        if(user == null) {
            User loggedInUser = authUtil.getCurrentUser();
            loggedInUser.setPhone(phone);
            userRepository.save(loggedInUser);

            return "Phone number updated successfully";
        } else {
            return "Phone number already exists";
        }
    }

    /**
     * Update user's basic information (name and/or phone)
     * Either field can be updated independently
     */
    public String updateUserBasicInfo(@Valid UpdateUserBasicInfoDto updateDto) {
        User loggedInUser = authUtil.getCurrentUser();

        boolean updated = false;
        StringBuilder message = new StringBuilder();

        // Update name if provided
        if (updateDto.getName() != null && !updateDto.getName().trim().isEmpty()) {
            loggedInUser.setName(updateDto.getName().trim());
            updated = true;
            message.append("Name updated successfully");
        }

        // Update phone if provided
        if (updateDto.getPhone() != null && !updateDto.getPhone().trim().isEmpty()) {
            // Check if phone already exists for another user
            User existingUser = userRepository.findUserByPhone(updateDto.getPhone());
            if (existingUser != null && !existingUser.getId().equals(loggedInUser.getId())) {
                throw new IllegalArgumentException("Phone number already exists");
            }

            loggedInUser.setPhone(updateDto.getPhone());
            updated = true;
            if (message.length() > 0) {
                message.append(" and phone number updated successfully");
            } else {
                message.append("Phone number updated successfully");
            }
        }

        if (updated) {
            userRepository.save(loggedInUser);
            return message.toString();
        } else {
            throw new IllegalArgumentException("No valid fields provided for update");
        }
    }
}
