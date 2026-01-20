package com.hitendra.turf_booking_backend.dto.service;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ResourceAvailabilityResponseDto {
    private List<SlotDto> slots;


    @Data
    @Builder
    public static class SlotDto {
        private String slotId;
        private String startTime;
        private String endTime;
        private String status;
        private Double price;
        private List<String> tags;
        private List<PriceComponent> priceBreakup;
        private String reason;
    }

    @Data
    @Builder
    public static class PriceComponent {
        private String label;
        private Double amount;
    }
}

