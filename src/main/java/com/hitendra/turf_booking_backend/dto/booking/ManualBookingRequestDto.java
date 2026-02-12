package com.hitendra.turf_booking_backend.dto.booking;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Simplified request DTO for admin manual walk-in bookings.
 * Only includes fields required for manual bookings (no user, no online payment complexities).
 *
 * Manual bookings:
 * - Are created by admin for walk-in customers
 * - Do NOT have an associated user_id
 * - Set created_by_admin_id to current admin
 * - Booking is immediately CONFIRMED
 * - Payment is recorded as-is without validation
 * - No payment webhook required
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualBookingRequestDto {

    /**
     * List of encrypted slot keys from slot availability response.
     * Each slotKey contains: serviceId, activityCode, date, startTime, endTime, resourceIds, etc.
     * Backend will assign an available resource automatically.
     * Required for booking.
     */
    @NotEmpty(message = "At least one slot must be selected")
    private List<String> slotKeys;

    /**
     * Whether to allow splitting the booking across multiple resources
     * if a single resource is not available for all consecutive slots.
     * Optional, defaults to false for single resource preference.
     */
    private Boolean allowSplit;

    /**
     * Amount paid online/in advance by the customer (via UPI/card to admin).
     * Optional - can be 0 for full cash payment at venue.
     * If not provided, defaults to 0.
     */
    private BigDecimal onlineAmountPaid;

    /**
     * Amount collected at venue (cash or digital payment to admin).
     * Can be the full booking amount or partial if online payment was made.
     * Optional - if not provided, defaults to 0.
     */
    private BigDecimal venueAmountCollected;

    /**
     * Optional notes/remarks for this booking.
     * Useful for tracking why this booking was made, special requests, etc.
     * E.g., "Birthday party", "Corporate event", "Walk-in customer", etc.
     */
    private String remarks;
}


