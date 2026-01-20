package com.hitendra.turf_booking_backend.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceCalculationResponse {
    private Double distanceInKilometers;
    private Double distanceInMeters;
    private Double distanceInMiles;
    private String formattedDistance;
}

