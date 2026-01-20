package com.hitendra.turf_booking_backend.dto.service;

import lombok.Data;
import java.time.LocalTime;

@Data
public class SlotDto {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double price; // Price for this specific turf-slot combination
    private boolean enabled; // Whether this slot is enabled for this turf
}
