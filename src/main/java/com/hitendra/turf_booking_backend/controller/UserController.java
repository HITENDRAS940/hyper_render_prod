package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.UserBookingDto;
import com.hitendra.turf_booking_backend.dto.user.UpdateUserBasicInfoDto;
import com.hitendra.turf_booking_backend.dto.user.UpdateUserProfileRequest;
import com.hitendra.turf_booking_backend.dto.user.UserProfileDto;
import com.hitendra.turf_booking_backend.security.service.UserDetailsImplementation;
import com.hitendra.turf_booking_backend.service.BookingService;
import com.hitendra.turf_booking_backend.service.UserProfileService;
import com.hitendra.turf_booking_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User Operations", description = "User-related operations including profile and bookings")
public class UserController {

    private final UserService userService;

    private final BookingService bookingService;

    private final UserProfileService userProfileService;

    @PutMapping("/basic-info")
    @Operation(summary = "Update user basic info",
               description = "Update the name and/or phone number of the current logged-in user. Both fields are optional.")
    public ResponseEntity<String> updateUserBasicInfo(@Valid @RequestBody UpdateUserBasicInfoDto updateDto) {
        String message = userService.updateUserBasicInfo(updateDto);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get user bookings",
               description = "Get all bookings for the current logged-in user with turf name, status, date, slots, and amount")
    public ResponseEntity<List<UserBookingDto>> getUserBookings() {
        List<UserBookingDto> bookings = bookingService.getCurrentUserBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/last")
    @Operation(summary = "Get last booking",
               description = "Get the most recent booking for the current logged-in user with turf name, status, date, slots, and amount. Returns null if user has no bookings.")
    public ResponseEntity<UserBookingDto> getLastBooking() {
        UserBookingDto lastBooking = bookingService.getLastUserBooking();
        if (lastBooking == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(lastBooking);
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking details", description = "Get detailed information about a specific booking")
    public ResponseEntity<BookingResponseDto> getBooking(@PathVariable Long bookingId) {
        BookingResponseDto booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(booking);
    }

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
