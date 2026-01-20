package com.hitendra.turf_booking_backend.dto.service;

import lombok.Data;
import java.util.List;

@Data
public class DeleteImagesRequest {
    private List<String> imageUrls;
}

