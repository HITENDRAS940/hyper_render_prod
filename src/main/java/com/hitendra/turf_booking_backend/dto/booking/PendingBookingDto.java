package com.hitendra.turf_booking_backend.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingBookingDto {
    private Long id;
    private String reference;
    private Long serviceId;
    private String serviceName;
    private Long resourceId;
    private String resourceName;
    private String startTime;
    private String endTime;
    private LocalDate bookingDate;
    private Double amount;
    private String status;
    private Instant createdAt;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        private String phone;
    }
}

