package com.hitendra.turf_booking_backend.dto.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

@Data
public class CreateServiceResourceRequest {
    @NotNull
    private Long serviceId;

    @NotBlank
    private String name;

    private String description;

    private Boolean enabled = true;

    // ==================== Slot Configuration Fields ====================
    // These fields are required to create a default slot configuration for the resource

    @NotNull(message = "Opening time is required")
    private LocalTime openingTime;

    @NotNull(message = "Closing time is required")
    private LocalTime closingTime;

    @NotNull(message = "Slot duration (in minutes) is required")
    private Integer slotDurationMinutes;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    private Double basePrice;

    // ==================== Activity Assignment Fields ====================
    // Activities that this resource supports (e.g., ["CRICKET", "FOOTBALL"])

    private List<String> activityCodes;
}
