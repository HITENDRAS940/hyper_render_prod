package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.CancellationResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.RefundHistoryDto;
import com.hitendra.turf_booking_backend.dto.booking.RefundPreviewDto;
import com.hitendra.turf_booking_backend.dto.coupon.CouponApplyResponseDto;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.service.CouponService;
import com.hitendra.turf_booking_backend.service.RefundService;
import com.hitendra.turf_booking_backend.util.AuthUtil;
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
public class BookingController {
    private final RefundService refundService;
    private final CouponService couponService;
    private final AuthUtil authUtil;

    /**
     * Refund History API (Read-Only)
     * Returns all refunds for the currently authenticated user, ordered by most
     * recent first.
     */
    @GetMapping("/refund-history")
    @Operation(summary = "Get refund history for current user", description = "Returns all refund records for the authenticated user, ordered by most recent first. "
            +
            "Includes refund status, amounts, booking details, and processing timestamps. " +
            "Read-only - no side effects.")
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
    @Operation(summary = "Get cancellation refund preview", description = "Calculate and return refundable amount for a booking WITHOUT modifying database. "
            +
            "Use this API to show user what they will receive before confirming cancellation. " +
            "Safe to call multiple times - no side effects.")
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
    @Operation(summary = "Cancel booking and initiate refund", description = "Cancel a confirmed booking and initiate refund. "
            +
            "Refund amount is RE-CALCULATED on backend (never trusts frontend). " +
            "Updates booking status to CANCELLED_BY_USER and creates refund record.")
    public ResponseEntity<CancellationResponseDto> cancelBooking(@PathVariable Long bookingId) {
        CancellationResponseDto response = refundService.cancelBookingWithRefund(bookingId);
        return ResponseEntity.ok(response);
    }

    /**
     * Apply Coupon to Booking API
     */
    @PostMapping("/{bookingId}/apply-coupon")
    @Operation(summary = "Apply coupon to pending booking",
               description = "Validates and applies a coupon code to a PENDING booking. " +
                             "Returns only the revised amount breakdown — all amounts are persisted to DB.")
    public ResponseEntity<CouponApplyResponseDto> applyCoupon(
            @PathVariable Long bookingId,
            @RequestParam String code) {
        User currentUser = authUtil.getCurrentUser();
        CouponApplyResponseDto response = couponService.applyCoupon(bookingId, code, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove Coupon from Booking API
     * Removes the applied coupon and restores the booking to its original amount.
     * Safe to call multiple times - returns error if no coupon is applied.
     */
    @DeleteMapping("/{bookingId}/remove-coupon")
    @Operation(summary = "Remove applied coupon from pending booking",
               description = "Removes a previously applied coupon from a PENDING booking. " +
                             "Reverts the booking to its original amount and recalculates the amount breakdown. " +
                             "All amounts are persisted to DB.")
    public ResponseEntity<CouponApplyResponseDto> removeCoupon(@PathVariable Long bookingId) {
        User currentUser = authUtil.getCurrentUser();
        CouponApplyResponseDto response = couponService.removeCoupon(bookingId, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Reapply Different Coupon API
     * Removes the current coupon and applies a new one in a single operation.
     * Useful when user wants to try a different coupon code.
     */
    @PostMapping("/{bookingId}/reapply-coupon")
    @Operation(summary = "Remove current coupon and apply a different one",
               description = "Convenience endpoint to replace an applied coupon with a different code. " +
                             "Removes the existing coupon and validates + applies the new coupon code in a single operation. " +
                             "Returns the revised amount breakdown with the new coupon applied. " +
                             "If the new coupon is invalid or the booking doesn't have an applied coupon, returns an appropriate error.")
    public ResponseEntity<CouponApplyResponseDto> reapplyCoupon(
            @PathVariable Long bookingId,
            @RequestParam String code) {
        User currentUser = authUtil.getCurrentUser();
        // First remove the existing coupon (if any)
        couponService.removeCoupon(bookingId, currentUser);
        // Then apply the new coupon
        CouponApplyResponseDto response = couponService.applyCoupon(bookingId, code, currentUser);
        return ResponseEntity.ok(response);
    }
}
