package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceCardDto {
    private Long id;
    private String name;
    private String location;
    private boolean availability;
    private List<String> images;
    private String description;

    // Google Places rating (cached, updated daily)
    private Double googleRating;
    private Integer googleReviewCount;
}
