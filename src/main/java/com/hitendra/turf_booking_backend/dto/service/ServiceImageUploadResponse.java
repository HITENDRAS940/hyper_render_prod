package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceImageUploadResponse {
    private Long serviceId;
    private String message;
    private List<String> uploadedImageUrls;
    private int totalImages;
}
