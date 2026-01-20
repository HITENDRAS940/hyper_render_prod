package com.hitendra.turf_booking_backend.dto.slot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Represents a dynamically generated slot from ResourceSlotConfig.
 * NOT stored in database - generated on-the-fly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedSlot {

    /**
     * Start time of the slot
     */
    private LocalTime startTime;

    /**
     * End time of the slot
     */
    private LocalTime endTime;

    /**
     * Duration in minutes
     */
    private Integer durationMinutes;

    /**
     * Display name (e.g., "6:00 AM - 7:00 AM")
     */
    private String displayName;

    /**
     * Base price from config
     */
    private Double basePrice;

    /**
     * Display order (0-based index)
     */
    private Integer displayOrder;
}

