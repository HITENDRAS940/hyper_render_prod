package com.hitendra.turf_booking_backend.dto.service;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

/**
 * Request DTO for creating/updating ResourceSlotConfig
 */
@Data
public class ResourceSlotConfigRequest {
    @NotNull
    private Long resourceId;

    @NotNull
    private LocalTime openingTime;

    @NotNull
    private LocalTime closingTime;

    @NotNull
    private Integer slotDurationMinutes;

    @NotNull
    private Double basePrice;

    private Double weekendPriceMultiplier;
}

