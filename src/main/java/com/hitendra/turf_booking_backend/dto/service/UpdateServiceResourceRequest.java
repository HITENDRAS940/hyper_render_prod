package com.hitendra.turf_booking_backend.dto.service;

import lombok.Data;

@Data
public class UpdateServiceResourceRequest {
    private String name;
    private String description;
    private Boolean enabled;
}

