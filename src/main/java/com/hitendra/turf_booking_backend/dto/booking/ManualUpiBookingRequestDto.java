package com.hitendra.turf_booking_backend.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for creating a manual UPI payment booking with soft-locking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualUpiBookingRequestDto {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotBlank(message = "Activity code is required")
    private String activityCode;

    @NotNull(message = "Booking date is required")
    private LocalDate bookingDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /**
     * Optional client-generated request ID for idempotency.
     * If provided, duplicate requests with same ID will return existing booking.
     */
    private String clientRequestId;
}

