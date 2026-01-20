package com.hitendra.turf_booking_backend.dto.booking;

import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for polling booking and payment status.
 * Used by frontend loading screens to track payment progress.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStatusResponse {
    private Long bookingId;
    private String reference;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private Instant paymentInitiatedAt;
    private Instant paymentCompletedAt;
    private String message;
    private Boolean isCompleted; // true if final state (CONFIRMED/CANCELLED/FAILED)
}

