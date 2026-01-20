package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.DayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * DTO for ResourcePriceRule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePriceRuleDto {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private DayType dayType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double basePrice;
    private Double extraCharge;
    private String reason;
    private Integer priority;
    private boolean enabled;
}

