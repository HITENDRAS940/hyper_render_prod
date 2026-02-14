package com.hitendra.turf_booking_backend.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Today's revenue report for a specific service
 * Shows total bookings, total revenue, and amount due today
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTodayRevenueDto {
    // Service Information
    private Long serviceId;
    private String serviceName;

    // Today's Metrics
    private LocalDate date;
    private Long totalBookingsToday;
    private Double totalRevenueToday;           // Total amount (booking amount)
    private Double amountDueToday;              // Online amount paid (due/received)
    private Double amountPendingAtVenue;        // Venue amount due (not collected yet)

    // Additional Info
    private String currency;
}

