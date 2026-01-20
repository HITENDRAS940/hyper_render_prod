package com.hitendra.turf_booking_backend.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Complete revenue report for an admin
 * Shows total revenue with breakdown by service and resource
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevenueReportDto {
    // Admin Information
    private Long adminId;
    private String adminName;
    private String adminPhone;

    // Overall Summary
    private Long totalBookings;
    private Double totalRevenue;
    private Double averageRevenuePerBooking;

    // Service-wise breakdown
    private List<ServiceRevenueDto> serviceRevenues;

    // Metadata
    private LocalDateTime generatedAt;
    private String currency;
}

