package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.PricingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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

    private List<String> activityCodes;

    // ==================== Pricing Type Fields ====================

    /**
     * Pricing strategy. Defaults to PER_SLOT if not provided.
     * PER_SLOT   — flat rate per slot (e.g. Turf).
     * PER_PERSON — price × numberOfPersons (e.g. Bowling, PS5).
     */
    private PricingType pricingType = PricingType.PER_SLOT;

    /**
     * Maximum persons allowed per booking.
     * Only meaningful for PER_PERSON resources. Null = no limit.
     */
    @Min(value = 1, message = "maxPersonAllowed must be at least 1 if specified")
    private Integer maxPersonAllowed;
}
