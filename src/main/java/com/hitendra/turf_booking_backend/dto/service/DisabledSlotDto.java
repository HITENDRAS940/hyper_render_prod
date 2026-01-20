package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisabledSlotDto {
    private Long id;
    private Long resourceId;
    private String resourceName;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate date;
    private String reason;
}
