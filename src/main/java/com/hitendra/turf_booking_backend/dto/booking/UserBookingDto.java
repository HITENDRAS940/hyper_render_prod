package com.hitendra.turf_booking_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingDto {
    private Long id;
    private Long serviceId;
    private String serviceName;
    private Long resourceId;
    private String resourceName;
    private String status;
    private LocalDate date;
    private List<SlotTimeDto> slots;
    private Double totalAmount;
    private Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotTimeDto {
        private String startTime;
        private String endTime;
    }
}
