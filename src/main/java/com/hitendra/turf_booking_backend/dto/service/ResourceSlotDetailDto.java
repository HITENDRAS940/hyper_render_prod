package com.hitendra.turf_booking_backend.dto.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hitendra.turf_booking_backend.entity.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for detailed resource slot information including availability status.
 * Used by frontend to display slot status (booked, disabled, available) without extra resource/service info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceSlotDetailDto {

    // Slot identity (format: resourceId-date-startTime)
    private String slotId;
    private String startTime;           // Formatted: "06:00 AM"
    private String endTime;             // Formatted: "07:00 AM"
    private String displayName;         // e.g., "6:00 AM - 7:00 AM"
    private Integer durationMinutes;    // Slot duration in minutes

    // Pricing
    private Double basePrice;           // Base price without fees
    private Double totalPrice;          // Price with 2% platform fee
    private Double price;               // Alias for totalPrice

    // Status
    @Builder.Default
    private SlotStatus status = SlotStatus.AVAILABLE;  // AVAILABLE, BOOKED, DISABLED

    // Additional info
    private String statusReason;        // e.g., "Booked by another user", "Admin disabled", "Maintenance"
    private Boolean isEnabled;          // Whether slot is enabled for booking
    private LocalDate slotDate;         // The date this slot is for
}

