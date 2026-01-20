package com.hitendra.turf_booking_backend.dto.service;

import lombok.Data;
import java.util.List;

@Data
public class ServiceDto {
    private Long id;
    private String name;
    private String location;
    private String city;
    private Double latitude;
    private Double longitude;
    private String description;
    private String contactNumber; // Need to be removed in production
    private boolean availability;
    private  List<String> amenities;
    private List<String> images;
    private List<String> activities;
}
