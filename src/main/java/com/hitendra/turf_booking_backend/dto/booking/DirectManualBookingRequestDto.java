package com.hitendra.turf_booking_backend.dto.booking;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Direct manual booking request DTO for admin walk-in bookings.
 * No slot key generation required - admin provides all booking details directly.
 * Booking is immediately CONFIRMED with:
 * - status: CONFIRMED
 * - payment_mode: MANUAL
 * - payment_source: BY_ADMIN
 * - created_by_admin_id: current admin
 * - user_id: NULL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectManualBookingRequestDto {

    /**
     * Service ID for which booking is made
     */
    @NotNull(message = "Service ID is required")
    private Long serviceId;

    /**
     * Resource ID to book (must be available for the time slot)
     */
    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    /**
     * Activity code (e.g., CRICKET, FOOTBALL, TENNIS)
     */
    @NotNull(message = "Activity code is required")
    private String activityCode;

    /**
     * Booking date
     */
    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    /**
     * Start time of the booking
     */
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    /**
     * End time of the booking
     */
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /**
     * Total booking amount
     */
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    /**
     * Amount paid online (UPI/Card to admin)
     * Optional - defaults to 0 if not provided
     */
    private BigDecimal onlineAmountPaid;

    /**
     * Amount collected at venue (cash/UPI)
     * Optional - defaults to 0 if not provided
     */
    private BigDecimal venueAmountCollected;

    /**
     * Optional remarks/notes for this booking
     * E.g., customer name, contact info, special requests
     */
    private String remarks;
}

