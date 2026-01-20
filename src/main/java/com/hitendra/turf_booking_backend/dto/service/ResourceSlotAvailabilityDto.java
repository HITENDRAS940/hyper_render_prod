package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for slot availability with pricing for a specific date
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSlotAvailabilityDto {
    private Long slotId;
    private Long resourceId;
    private String resourceName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String displayName;
    private Double price;           // Final calculated price for this date
    private Double basePrice;       // Base price without modifiers
    private boolean available;      // Whether the slot is available for booking
    private String status;          // Status: AVAILABLE, BOOKED, DISABLED_BY_ADMIN
    private boolean isWeekend;      // Whether the query date is weekend
    private boolean isPeakHour;     // Whether peak hour pricing applies
    private LocalDate date;         // The date for which availability was checked
}
