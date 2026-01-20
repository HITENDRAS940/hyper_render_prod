package com.hitendra.turf_booking_backend.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for service with distance information
 * Used in filter services by distance response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWithDistanceDto {
    private Long serviceId;
    private String serviceName;
    private String city;
    private String location;
    private String description;
    private Double distanceInKilometers;
    private Double distanceInMeters;
    private Double distanceInMiles;
    private String formattedDistance;
}

