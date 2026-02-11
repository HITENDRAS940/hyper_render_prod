package com.hitendra.turf_booking_backend.dto.slot;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO for disabling a single slot or time range for a resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisableSlotRequestDto {

    /**
     * Resource ID to disable slot for
     */
    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    /**
     * Date on which to disable the slot(s)
     */
    @NotNull(message = "Date is required")
    private LocalDate date;

    /**
     * Start time of the slot or range to disable
     */
    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    /**
     * End time of the slot or range to disable
     * If not provided, will disable only the single slot starting at startTime
     */
    private LocalTime endTime;

    /**
     * Reason for disabling (e.g., "Maintenance", "Private event", "Cleaning")
     */
    private String reason;
}

