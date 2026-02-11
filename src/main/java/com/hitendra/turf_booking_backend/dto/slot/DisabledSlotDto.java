package com.hitendra.turf_booking_backend.dto.slot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO for disabled slot information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisabledSlotDto {

    private Long id;
    private Long resourceId;
    private String resourceName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String disabledByAdminName;
    private Instant createdAt;
    private String displayTime; // e.g., "6:00 AM - 7:00 AM"
}

