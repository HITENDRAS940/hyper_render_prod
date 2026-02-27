package com.hitendra.turf_booking_backend.dto.booking;

import jakarta.validation.constraints.Min;
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

    /**
     * Number of persons for PER_PERSON pricing resources (e.g. PS5, Bowling).
     * Required when the resource's pricingType = PER_PERSON.
     * Ignored (treated as 1) for PER_SLOT resources.
     */
    @Min(value = 1, message = "Number of persons must be at least 1")
    private Integer numberOfPersons;
}


