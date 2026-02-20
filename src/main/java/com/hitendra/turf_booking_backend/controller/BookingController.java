package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.CancellationResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.RefundHistoryDto;
import com.hitendra.turf_booking_backend.dto.booking.RefundPreviewDto;
import com.hitendra.turf_booking_backend.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Bookings", description = "User booking APIs")
@PreAuthorize("hasRole('USER')")
public class  BookingController {
    private final RefundService refundService;

    /**
     * Refund History API (Read-Only)
     * Returns all refunds for the currently authenticated user, ordered by most recent first.
     */
    @GetMapping("/refund-history")
    @Operation(
        summary = "Get refund history for current user",
        description = "Returns all refund records for the authenticated user, ordered by most recent first. " +
                     "Includes refund status, amounts, booking details, and processing timestamps. " +
                     "Read-only - no side effects."
    )
    public ResponseEntity<List<RefundHistoryDto>> getRefundHistory() {
        List<RefundHistoryDto> history = refundService.getRefundHistory();
        return ResponseEntity.ok(history);
    }

    /**
     * Refund Preview API (Read-Only)
     * Returns refund calculation without modifying any database.
     * Safe to call multiple times.
     */
    @GetMapping("/{bookingId}/cancel-preview")
    @Operation(
        summary = "Get cancellation refund preview",
        description = "Calculate and return refundable amount for a booking WITHOUT modifying database. " +
                     "Use this API to show user what they will receive before confirming cancellation. " +
                     "Safe to call multiple times - no side effects."
    )
    public ResponseEntity<RefundPreviewDto> getCancelPreview(@PathVariable Long bookingId) {
        RefundPreviewDto preview = refundService.getRefundPreview(bookingId);
        return ResponseEntity.ok(preview);
    }

    /**
     * Cancel Booking API (DB Update + Refund Initiation)
     * Re-calculates refund on server side and processes cancellation.
     * Never trusts frontend refund values.
     */
    @PostMapping("/{bookingId}/cancel")
    @Operation(
        summary = "Cancel booking and initiate refund",
        description = "Cancel a confirmed booking and initiate refund. " +
                     "Refund amount is RE-CALCULATED on backend (never trusts frontend). " +
                     "Updates booking status to CANCELLED_BY_USER and creates refund record."
    )
    public ResponseEntity<CancellationResponseDto> cancelBooking(@PathVariable Long bookingId) {
        CancellationResponseDto response = refundService.cancelBookingWithRefund(bookingId);
        return ResponseEntity.ok(response);
    }
}
