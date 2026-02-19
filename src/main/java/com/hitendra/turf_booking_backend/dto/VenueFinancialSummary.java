package com.hitendra.turf_booking_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@Builder
public class VenueFinancialSummary {
    private BigDecimal grossRevenue;
    private BigDecimal cancelledRevenue;
    private BigDecimal refundedRevenue;
    private BigDecimal netRevenue;
    private BigDecimal advancePending;
    private BigDecimal advanceReceived;
    private BigDecimal cashCollected;
    private BigDecimal cashBalance;
    private BigDecimal bankBalance;
    private BigDecimal netProfit;

    // Helper to ensure non-null values for easier frontend handling
    public static VenueFinancialSummary zero() {
        return VenueFinancialSummary.builder()
                .grossRevenue(BigDecimal.ZERO)
                .cancelledRevenue(BigDecimal.ZERO)
                .refundedRevenue(BigDecimal.ZERO)
                .netRevenue(BigDecimal.ZERO)
                .advancePending(BigDecimal.ZERO)
                .advanceReceived(BigDecimal.ZERO)
                .cashCollected(BigDecimal.ZERO)
                .cashBalance(BigDecimal.ZERO)
                .bankBalance(BigDecimal.ZERO)
                .netProfit(BigDecimal.ZERO)
                .build();
    }
}

