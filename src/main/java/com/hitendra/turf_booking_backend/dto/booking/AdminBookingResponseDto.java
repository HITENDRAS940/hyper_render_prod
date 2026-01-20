package com.hitendra.turf_booking_backend.dto.booking;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
public class AdminBookingResponseDto {
    private Long id;
    private String reference;
    private Double amount;  // Total amount for all slots
    private String status;
    private String turfName;
    private String slotTime;  // Kept for backward compatibility (single slot)
    private List<SlotDetails> slots;  // New: Multiple slots with details
    private LocalDate bookingDate;
    private Instant createdAt;

    @Data
    public static class SlotDetails {
        private Long slotId;
        private String startTime;
        private String endTime;
        private Double price;
    }
}
