package com.hitendra.turf_booking_backend.dto.service;

import lombok.Data;
import java.util.List;

@Data
public class CreateServiceRequest {
    private String name;
    private String location;
    private String city;
    private Double latitude;
    private Double longitude;
    private String description;
    private String contactNumber;
    private List<String> activityCodes;  // List of activity codes (e.g., ["CRICKET", "FOOTBALL"])
    private List<String> amenities;      // List of amenities (e.g., ["Parking", "WiFi", "Cafeteria", "Lighting"])

    /**
     * Whether user cancellation (with refund) is permitted for bookings under this service.
     * Defaults to true when not provided.
     */
    private Boolean refundAllowed = true;

    /**
     * Google Places place_id for this service/venue.
     * Used to fetch rating and review count from Google Places API.
     */
    private String googlePlaceId;
}

