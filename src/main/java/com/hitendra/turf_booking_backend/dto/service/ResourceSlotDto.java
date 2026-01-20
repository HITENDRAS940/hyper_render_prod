package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO for ResourceSlot - represents an individual bookable slot for a resource
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSlotDto {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String displayName;
    private Double basePrice;
    private Double weekdayPrice;
    private Double weekendPrice;
    private Integer displayOrder;
    private Integer durationMinutes;
    private boolean enabled;
}

