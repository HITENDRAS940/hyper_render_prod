package com.hitendra.turf_booking_backend.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Admin dashboard statistics showing bookings and revenue breakdown
 * by online vs offline for today and current month
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsDto {

    private BookingStats todayBooking;
    private BookingStats monthlyBooking;
    private RevenueStats todayRevenue;
    private RevenueStats monthlyRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingStats {
        private Long online;
        private Long offline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        private BigDecimal online;
        private BigDecimal offline;
    }
}

