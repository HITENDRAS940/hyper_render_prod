package com.hitendra.turf_booking_backend.dto.service;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationUrlRequest {
    @NotBlank(message = "Location URL is required")
    private String locationUrl;
}

