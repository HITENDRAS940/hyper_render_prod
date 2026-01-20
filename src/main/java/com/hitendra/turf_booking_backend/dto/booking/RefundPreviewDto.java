package com.hitendra.turf_booking_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for refund preview response.
 * This is a read-only preview - no DB changes are made.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPreviewDto {

    /**
     * Whether the booking can be cancelled
     */
    private boolean canCancel;

    /**
     * Booking ID
     */
    private Long bookingId;

    /**
     * Booking reference
     */
    private String bookingReference;

    /**
     * Original amount paid
     */
    private BigDecimal originalAmount;

    /**
     * Minutes remaining before slot starts
     */
    private long minutesBeforeSlot;

    /**
     * Refund percentage based on cancellation policy
     */
    private int refundPercent;

    /**
     * Calculated refund amount
     */
    private BigDecimal refundAmount;

    /**
     * Deduction amount (originalAmount - refundAmount)
     */
    private BigDecimal deductionAmount;

    /**
     * Currency code
     */
    @Builder.Default
    private String currency = "INR";

    /**
     * User-friendly message explaining the refund
     */
    private String message;

    /**
     * Refund policy message
     */
    private String policyMessage;

    /**
     * If cancellation is not allowed, this explains why
     */
    private String reasonNotAllowed;
}
