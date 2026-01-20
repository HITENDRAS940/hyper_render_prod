package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.DayType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * Request DTO for creating/updating ResourcePriceRule
 */
@Data
public class ResourcePriceRuleRequest {
    @NotNull
    private Long resourceId;

    @NotNull
    private DayType dayType;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private Double basePrice;      // Override base price for this time range

    private Double extraCharge;    // Extra charge on top of base price

    private String reason;         // e.g., "Night lighting", "Peak hours"

    private Integer priority;      // Higher priority wins (default: 1)
}

