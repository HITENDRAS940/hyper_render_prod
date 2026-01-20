package com.hitendra.turf_booking_backend.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Revenue breakdown for a single service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRevenueDto {
    private Long serviceId;
    private String serviceName;
    private Long totalBookings;
    private Double totalRevenue;
    private Double averageRevenuePerBooking;
    private List<ResourceRevenueDto> resourceRevenues;
}

