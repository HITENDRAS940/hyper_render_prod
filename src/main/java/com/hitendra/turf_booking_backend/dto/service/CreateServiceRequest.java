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
}

