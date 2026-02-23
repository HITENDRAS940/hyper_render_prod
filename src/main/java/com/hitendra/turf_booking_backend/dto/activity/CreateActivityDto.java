package com.hitendra.turf_booking_backend.dto.activity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateActivityDto {

    @NotBlank(message = "Activity code is required")
    private String code; // e.g., FOOTBALL, CRICKET

    @NotBlank(message = "Activity name is required")
    private String name; // e.g., Football, Cricket
}

