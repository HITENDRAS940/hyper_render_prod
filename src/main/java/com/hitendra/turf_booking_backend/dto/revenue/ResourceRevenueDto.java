package com.hitendra.turf_booking_backend.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Revenue breakdown for a single resource
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRevenueDto {
    private Long resourceId;
    private String resourceName;
    private Long bookingCount;
    private Double totalRevenue;
    private Double averageRevenuePerBooking;
}

