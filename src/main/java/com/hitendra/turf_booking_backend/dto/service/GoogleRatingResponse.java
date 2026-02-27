package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Google Places API (New) rating response.
 * Maps the JSON response from:
 * GET https://places.googleapis.com/v1/places/{PLACE_ID}
 * with FieldMask: rating,userRatingCount
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleRatingResponse {

    private Double rating;
    private Integer userRatingCount;
}

