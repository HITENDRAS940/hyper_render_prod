package com.hitendra.turf_booking_backend.dto.location;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterServicesByDistanceRequest {
    @NotNull(message = "User latitude is required")
    private Double userLatitude;

    @NotNull(message = "User longitude is required")
    private Double userLongitude;

    @NotNull(message = "Max distance in km is required")
    private Double maxDistanceKm;

    private Double minDistanceKm;  // Optional, defaults to 0

    private String city;  // Optional, filters by city if provided
}

