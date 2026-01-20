package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotStatusResponseDto {
    private Long serviceId;
    private LocalDate date;
    private List<Long> disabled;  // Slot IDs that are disabled (either permanently or for this date)
    private List<Long> booked;    // Slot IDs that are booked for this date
}
