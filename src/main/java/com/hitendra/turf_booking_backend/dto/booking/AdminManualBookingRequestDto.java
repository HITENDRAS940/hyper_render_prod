package com.hitendra.turf_booking_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manual booking request DTO for admin walk-in bookings.
 * Similar to SlotBookingRequestDto but for admin-initiated bookings.
 * These bookings are completed immediately without payment webhooks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminManualBookingRequestDto {

    /**
     * List of encrypted slot keys containing booking details.
     * Each slotKey contains: serviceId, activityCode, date, startTime, endTime, resourceIds, etc.
     */
    private List<String> slotKeys;

    /**
     * Whether to allow splitting the booking across multiple resources
     * if a single resource is not available for all slots.
     */
    private Boolean allowSplit;

    /**
     * Amount paid online (if customer paid via UPI/card to admin)
     * Can be 0 if full payment at venue
     */
    private BigDecimal onlineAmountPaid;

    /**
     * Amount collected at venue (cash/UPI to admin)
     * Can be the full amount for walk-in bookings
     */
    private BigDecimal venueAmountCollected;

    /**
     * Customer name (optional for walk-in bookings)
     */
    private String customerName;

    /**
     * Customer phone (optional for walk-in bookings)
     */
    private String customerPhone;

    /**
     * Notes/remarks for this booking
     */
    private String remarks;
}

