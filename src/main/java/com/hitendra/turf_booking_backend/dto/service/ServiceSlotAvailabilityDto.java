package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSlotAvailabilityDto {

    private Long serviceSlotId;
    private Long serviceId;
    private String serviceName;
    private Long slotId;
    private String slotName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double price;
    private boolean enabled;
    private Set<LocalDate> bookedDates;
    private boolean availableForDate; // for specific date queries
    private LocalDate queryDate; // the date that was queried for availability
}
