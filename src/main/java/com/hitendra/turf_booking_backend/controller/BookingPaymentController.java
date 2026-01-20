package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.BookingPaymentRequest;
import com.hitendra.turf_booking_backend.dto.booking.BookingPaymentResponse;
import com.hitendra.turf_booking_backend.service.BookingPaymentService;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for booking with integrated payment.
 */
@RestController
@RequestMapping("/api/bookings/pay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Booking Payment", description = "Booking with wallet/online payment APIs")
public class BookingPaymentController {

    private final BookingPaymentService bookingPaymentService;
    private final AuthUtil authUtil;

    @PostMapping
    @Operation(summary = "Create booking with payment",
            description = "Create a booking and process payment (wallet-only, online-only, or wallet+online)")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<BookingPaymentResponse> createBookingWithPayment(
            @Valid @RequestBody BookingPaymentRequest request) {

        Long userId = authUtil.getCurrentUserId();
        log.info("Booking request: userId={}, resourceId={}, method={}",
                userId, request.getResourceId(), request.getPaymentMethod());

        BookingPaymentResponse response = bookingPaymentService.createBookingWithPayment(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel booking with refund",
            description = "Cancel a booking and refund amount to wallet")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        Long userId = authUtil.getCurrentUserId();
        log.info("Cancellation request: userId={}, bookingId={}", userId, bookingId);

        bookingPaymentService.cancelBookingWithRefund(bookingId, userId);
        return ResponseEntity.ok().build();
    }
}

