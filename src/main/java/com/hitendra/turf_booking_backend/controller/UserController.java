package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.UserBookingDto;
import com.hitendra.turf_booking_backend.dto.user.UserDto;
import com.hitendra.turf_booking_backend.service.BookingService;
import com.hitendra.turf_booking_backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User Operations", description = "User-related operations including profile and bookings")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @PostMapping("/setname")
    @Operation(summary = "Set user name", description = "Set name for a new user")
    public ResponseEntity<String> setName(
            @RequestBody UserDto userDto
            ) {
        return ResponseEntity.ok(userService.setNewUserName(userDto));
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
}
