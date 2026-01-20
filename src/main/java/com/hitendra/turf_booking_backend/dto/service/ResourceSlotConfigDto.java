package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO for ResourceSlotConfig.
 * Slots are generated dynamically, not stored in database.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSlotConfigDto {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Integer slotDurationMinutes;
    private Double basePrice;
    private boolean enabled;
    private Integer totalSlots;  // Number of slots that will be generated
}

