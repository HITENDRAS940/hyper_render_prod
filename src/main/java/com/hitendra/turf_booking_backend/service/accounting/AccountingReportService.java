package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.ProfitLossReportDto;
import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import com.hitendra.turf_booking_backend.repository.accounting.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ACCOUNTING REPORTS SERVICE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Generates financial reports:
 * - Profit & Loss Statement
 * - Cash Flow Report
 * - Expense Breakdown
 * - Inventory Profit Analysis
 *
 * IMPORTANT: All profit calculations use ledger data, not direct tables.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class AccountingReportService {

    private final ServiceRepository serviceRepository;
    private final BookingRepository bookingRepository;
    private final ExpenseRepository expenseRepository;
    private final CashLedgerRepository ledgerRepository;

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * GENERATE PROFIT & LOSS STATEMENT
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * This is the CORE financial report showing:
     * - All income sources (bookings + inventory sales)
     * - All expenses (by category)
     * - Net profit
     * - Cash flow
     *
     * CALCULATION METHODOLOGY:
     *
     * INCOME:
     * - Booking Revenue = SUM of confirmed bookings
     * - Inventory Sales = SUM of inventory sales
     * - Total Income = Booking Revenue + Inventory Sales
     *
     * EXPENSES:
     * - Total Expenses = SUM of all expense records
     * - Breakdown by category
     *
     * NET PROFIT:
     * - Net Profit = Total Income - Total Expenses - Inventory Purchases
     *
     * CASH FLOW:
     * - Derived from cash ledger (single source of truth)
     *
     * @param serviceId The service to generate report for
     * @param startDate Report start date
     * @param endDate Report end date
     * @return Complete P&L report
     */
    public ProfitLossReportDto generateProfitLossReport(
        Long serviceId, LocalDate startDate, LocalDate endDate) {

        log.info("Generating P&L report for service {} from {} to {}",
            serviceId, startDate, endDate);

        // Validate service
        Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        // Convert dates to Instant for ledger queries
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 1: CALCULATE INCOME
        // ═══════════════════════════════════════════════════════════════════════

        // Booking Revenue (from bookings table)
        Double bookingRevenue = bookingRepository.getTotalRevenueByServiceAndDateRange(
            serviceId, startDate, endDate);
        if (bookingRevenue == null) bookingRevenue = 0.0;

        Double totalIncome = bookingRevenue;

        log.info("INCOME - Bookings: {}, Total: {}",
            bookingRevenue, totalIncome);

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 2: CALCULATE EXPENSES
        // ═══════════════════════════════════════════════════════════════════════

        // Total Expenses
        Double totalExpenses = expenseRepository.getTotalExpensesByServiceAndDateRange(
            serviceId, startDate, endDate);
        if (totalExpenses == null) totalExpenses = 0.0;

        // Expense Breakdown by Category
        List<Object[]> breakdownData = expenseRepository.getExpenseBreakdownByCategory(
            serviceId, startDate, endDate);

        Map<String, Double> expenseBreakdown = new HashMap<>();
        for (Object[] row : breakdownData) {
            String category = (String) row[0];
            Double amount = (Double) row[1];
            expenseBreakdown.put(category, amount);
        }

        log.info("EXPENSES - Total: {}, Categories: {}",
            totalExpenses, expenseBreakdown.size());

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 3: CALCULATE NET PROFIT
        // ═══════════════════════════════════════════════════════════════════════

        // Net Profit = Total Income - Total Expenses
        Double netProfit = totalIncome - totalExpenses;

        // Profit Margin % = (Net Profit / Total Income) * 100
        Double profitMargin = totalIncome > 0 ? (netProfit / totalIncome) * 100 : 0.0;

        log.info("NET PROFIT: {}, Margin: {}%", netProfit, profitMargin);

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 4: CALCULATE CASH FLOW (from ledger)
        // ═══════════════════════════════════════════════════════════════════════

        // Opening balance (before start date)
        Double openingBalance = ledgerRepository.getCurrentBalance(serviceId).orElse(0.0);

        // Total cash in during period
        Double totalCashIn = ledgerRepository.getTotalCreditsByServiceAndDateRange(
            serviceId, startInstant, endInstant);
        if (totalCashIn == null) totalCashIn = 0.0;

        // Total cash out during period
        Double totalCashOut = ledgerRepository.getTotalDebitsByServiceAndDateRange(
            serviceId, startInstant, endInstant);
        if (totalCashOut == null) totalCashOut = 0.0;

        // Closing balance
        Double closingBalance = openingBalance + totalCashIn - totalCashOut;

        log.info("CASH FLOW - Opening: {}, In: {}, Out: {}, Closing: {}",
            openingBalance, totalCashIn, totalCashOut, closingBalance);

        // ═══════════════════════════════════════════════════════════════════════
        // BUILD REPORT DTO
        // ═══════════════════════════════════════════════════════════════════════

        return ProfitLossReportDto.builder()
            .serviceId(serviceId)
            .serviceName(service.getName())
            .startDate(startDate)
            .endDate(endDate)
            // Income
            .bookingRevenue(bookingRevenue)
            .totalIncome(totalIncome)
            // Expenses
            .totalExpenses(totalExpenses)
            .expenseBreakdown(expenseBreakdown)
            // Summary
            .netProfit(netProfit)
            .profitMargin(Math.round(profitMargin * 100.0) / 100.0)
            // Cash Flow
            .openingBalance(openingBalance)
            .totalCashIn(totalCashIn)
            .totalCashOut(totalCashOut)
            .closingBalance(closingBalance)
            .build();
    }
}

