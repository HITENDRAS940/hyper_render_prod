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
 * Unified request DTO for disabling slots.
 * Supports all disable operations in a single request:
 * - Single slot disable
 * - Time range disable (single day)
 * - Entire day disable
 * - Multiple days disable (date range)
 * - Multiple resources disable
 * - Service-wide disable
 *
 * Examples:
 *
 * 1. Disable single slot:
 * {
 *   "resourceIds": [1],
 *   "startDate": "2026-02-15",
 *   "startTime": "10:00",
 *   "reason": "Maintenance"
 * }
 *
 * 2. Disable time range (same day):
 * {
 *   "resourceIds": [1],
 *   "startDate": "2026-02-15",
 *   "startTime": "10:00",
 *   "endTime": "14:00",
 *   "reason": "Private event"
 * }
 *
 * 3. Disable entire day:
 * {
 *   "resourceIds": [1, 2],
 *   "startDate": "2026-02-15",
 *   "reason": "Holiday"
 * }
 *
 * 4. Disable date range (entire days):
 * {
 *   "serviceId": 1,
 *   "startDate": "2026-02-15",
 *   "endDate": "2026-02-20",
 *   "reason": "Annual maintenance"
 * }
 *
 * 5. Disable time range across multiple days:
 * {
 *   "resourceIds": [1],
 *   "startDate": "2026-02-15",
 *   "endDate": "2026-02-20",
 *   "startTime": "06:00",
 *   "endTime": "12:00",
 *   "reason": "Morning renovation"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedDisableSlotRequestDto {

    /**
     * List of resource IDs to disable slots for.
     * If empty or null, must provide serviceId.
     */
    private List<Long> resourceIds;

    /**
     * Service ID - applies to all resources in the service.
     * Used when resourceIds is empty/null.
     */
    private Long serviceId;

    /**
     * Start date for disabling slots (required).
     */
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    /**
     * End date for disabling slots (optional).
     * If not provided, only startDate is affected.
     * If provided, disables slots from startDate to endDate (inclusive).
     */
    private LocalDate endDate;

    /**
     * Start time for the slot(s) to disable (optional).
     * If not provided, disables entire day(s).
     * If provided without endTime, disables single slot starting at this time.
     * If provided with endTime, disables all slots in the time range.
     */
    private LocalTime startTime;

    /**
     * End time for the slot range to disable (optional).
     * Only used when startTime is provided.
     * If not provided, disables single slot or entire day depending on startTime.
     */
    private LocalTime endTime;

    /**
     * Reason for disabling (optional but recommended).
     * Examples: "Maintenance", "Private event", "Holiday", "Renovation"
     */
    private String reason;
}

