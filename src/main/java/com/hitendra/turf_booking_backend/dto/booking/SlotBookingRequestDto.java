package com.hitendra.turf_booking_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Intent-based booking request DTO.
 * Frontend sends encrypted slotKeys containing booking details.
 * All bookings are created with PENDING status and await payment via Razorpay.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotBookingRequestDto {

    /**
     * List of encrypted slot keys containing booking details.
     * Each slotKey contains: serviceId, activityCode, date, startTime, endTime, resourceIds, etc.
     */
    private java.util.List<String> slotKeys;

    /**
     * Idempotency key for retry handling.
     * If provided, duplicate requests with same key are rejected.
     */
    private String idempotencyKey;

    /**
     * Whether to allow splitting the booking across multiple resources
     * if a single resource is not available for all slots.
     */
    private Boolean allowSplit;

    /**
     * Payment method for this booking (e.g., "RAZORPAY", "WALLET", etc.).
     * All booking details (serviceId, activityCode, date, time) are decoded from slotKeys.
     */
    private String paymentMethod;
}


