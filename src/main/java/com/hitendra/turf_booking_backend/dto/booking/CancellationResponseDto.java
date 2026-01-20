package com.hitendra.turf_booking_backend.dto.booking;

import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for cancellation response after booking is cancelled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationResponseDto {

    /**
     * Whether cancellation was successful
     */
    private boolean success;

    /**
     * Booking ID
     */
    private Long bookingId;

    /**
     * Booking reference
     */
    private String bookingReference;

    /**
     * Updated booking status
     */
    private BookingStatus bookingStatus;

    /**
     * Original amount paid
     */
    private BigDecimal originalAmount;

    /**
     * Refund amount (may be 0 if no refund eligible)
     */
    private BigDecimal refundAmount;

    /**
     * Refund percentage applied
     */
    private int refundPercent;

    /**
     * Refund status
     */
    private RefundStatus refundStatus;

    /**
     * Refund ID for tracking
     */
    private Long refundId;

    /**
     * Where refund will be credited - RAZORPAY or WALLET
     */
    private String refundType;

    /**
     * Currency code
     */
    @Builder.Default
    private String currency = "INR";

    /**
     * User-friendly message
     */
    private String message;

    /**
     * When cancellation was processed
     */
    private Instant cancelledAt;
}
