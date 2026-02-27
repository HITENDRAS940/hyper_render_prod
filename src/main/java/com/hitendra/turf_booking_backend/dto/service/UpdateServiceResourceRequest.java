package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.PricingType;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateServiceResourceRequest {
    private String name;
    private String description;
    private Boolean enabled;

    /**
     * Update the pricing strategy. Leave null to keep existing value.
     */
    private PricingType pricingType;

    /**
     * Update the max persons allowed. Leave null to keep existing value.
     * Set to 0 to remove the limit (stored as null internally).
     */
    @Min(value = 1, message = "maxPersonAllowed must be at least 1 if specified")
    private Integer maxPersonAllowed;
}

