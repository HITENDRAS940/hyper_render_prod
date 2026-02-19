package com.hitendra.turf_booking_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ServiceFinancialSummary {
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
    public static ServiceFinancialSummary zero() {
        return ServiceFinancialSummary.builder()
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

