package com.hitendra.turf_booking_backend.dto.booking;

import com.hitendra.turf_booking_backend.entity.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO representing a single refund record in the user's refund history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundHistoryDto {

    /** Refund record ID */
    private Long refundId;

    /** Booking ID associated with this refund */
    private Long bookingId;

    /** Human-readable booking reference (e.g. BK-20240101-ABCD) */
    private String bookingReference;

    /** Name of the service/venue booked */
    private String serviceName;

    /** Date of the original booking slot */
    private String bookingDate;

    /** Slot time range (e.g. "10:00 - 11:00") */
    private String slotTime;

    /** Original amount paid for the booking */
    private BigDecimal originalAmount;

    /** Amount refunded (after applying cancellation policy) */
    private BigDecimal refundAmount;

    /** Refund percentage applied */
    private int refundPercent;

    /** Deduction amount (originalAmount - refundAmount) */
    private BigDecimal deductionAmount;

    /** Currency code (default: INR) */
    @Builder.Default
    private String currency = "INR";

    /** Refund destination: RAZORPAY or WALLET */
    private String refundType;

    /** Current status of the refund */
    private RefundStatus status;

    /** Razorpay refund ID (if applicable) */
    private String razorpayRefundId;

    /** When the refund was initiated */
    private Instant initiatedAt;

    /** When the refund was processed/completed (null if still pending) */
    private Instant processedAt;
}

