package com.hitendra.turf_booking_backend.dto.accounting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * Profit & Loss Statement DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitLossReportDto {

    private Long serviceId;
    private String serviceName;
    private LocalDate startDate;
    private LocalDate endDate;

    // INCOME
    private Double bookingRevenue;
    private Double totalIncome;

    // EXPENSES
    private Double totalExpenses;
    private Map<String, Double> expenseBreakdown; // Category → Amount


    // SUMMARY
    private Double netProfit;      // Total Income - Total Expenses
    private Double profitMargin;   // (Net Profit / Total Income) * 100

    // CASH FLOW
    private Double openingBalance;
    private Double totalCashIn;
    private Double totalCashOut;
    private Double closingBalance;
}

