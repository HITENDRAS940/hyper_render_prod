package com.hitendra.turf_booking_backend.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published when a booking is confirmed.
 * Triggers asynchronous invoice generation.
 *
 * ARCHITECTURE:
 * - Webhook confirms booking -> publishes this event -> returns 200 OK
 * - Async listener picks up event -> generates invoice in background
 * - Keeps webhook response fast and non-blocking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {

    /**
     * ID of the confirmed booking.
     */
    private Long bookingId;

    /**
     * Timestamp when event was created (for monitoring delays).
     */
    private Long timestamp;

    /**
     * Razorpay payment ID (for logging/tracking).
     */
    private String razorpayPaymentId;
}

