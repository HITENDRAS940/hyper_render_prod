package com.hitendra.turf_booking_backend.dto.slot;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO for bulk disabling multiple slots/ranges.
 * Useful for disabling entire days or multiple time ranges at once.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDisableSlotRequestDto {

    /**
     * List of resource IDs to disable slots for
     * If empty or null, applies to all resources of the service
     */
    private List<Long> resourceIds;

    /**
     * Service ID (required if resourceIds not provided)
     */
    private Long serviceId;

    /**
     * Start date of the range to disable
     */
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    /**
     * End date of the range to disable (inclusive)
     * If not provided, only startDate will be disabled
     */
    private LocalDate endDate;

    /**
     * Start time for each day (optional)
     * If not provided, will disable entire day (all slots)
     */
    private LocalTime startTime;

    /**
     * End time for each day (optional)
     * If not provided with startTime, will disable entire day
     */
    private LocalTime endTime;

    /**
     * Reason for bulk disabling
     */
    private String reason;
}

