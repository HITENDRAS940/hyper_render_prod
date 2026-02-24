package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.*;
import com.hitendra.turf_booking_backend.entity.AdminProfile;
import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.entity.accounting.Expense;
import com.hitendra.turf_booking_backend.entity.accounting.ExpensePaymentMode;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.RefundRepository;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseRepository;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceFinancialService {

    private final BookingRepository bookingRepository;
    private final RefundRepository refundRepository;
    private final ExpenseRepository expenseRepository;
    private final ServiceRepository serviceRepository;

    public ServiceFinancialSummary getDashboardSummary(Long serviceId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        Double grossRevDouble = bookingRepository.calculateGrossRevenue(serviceId, startDate, endDate);
        BigDecimal grossRevenue = grossRevDouble != null ? BigDecimal.valueOf(grossRevDouble) : BigDecimal.ZERO;

        Double cancelledRevDouble = bookingRepository.calculateCancelledRevenue(serviceId);
        BigDecimal cancelledRevenue = cancelledRevDouble != null ? BigDecimal.valueOf(cancelledRevDouble) : BigDecimal.ZERO;

        BigDecimal refundedRevenue = refundRepository.calculateRefundedRevenue(serviceId, startDate, endDate);
        if (refundedRevenue == null) refundedRevenue = BigDecimal.ZERO;

        BigDecimal netRevenue = grossRevenue.subtract(refundedRevenue);

        BigDecimal advancePending = bookingRepository.calculateAdvancePending(serviceId);
        if (advancePending == null) advancePending = BigDecimal.ZERO;

        BigDecimal advanceReceived = bookingRepository.calculateAdvanceReceived(serviceId);
        if (advanceReceived == null) advanceReceived = BigDecimal.ZERO;

        BigDecimal cashCollected = bookingRepository.calculateCashCollected(serviceId);
        if (cashCollected == null) cashCollected = BigDecimal.ZERO;

        // Expenses are at admin level - get the service owner admin
        Service service = serviceRepository.findById(serviceId).orElse(null);
        Long adminProfileId = service != null ? service.getCreatedBy().getId() : null;

        BigDecimal totalExpenses = adminProfileId != null
                ? expenseRepository.sumAmountByAdminProfileId(adminProfileId) : BigDecimal.ZERO;
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal expensesCash = adminProfileId != null
                ? expenseRepository.sumAmountByAdminProfileIdAndPaymentMode(adminProfileId, ExpensePaymentMode.CASH) : BigDecimal.ZERO;
        if (expensesCash == null) expensesCash = BigDecimal.ZERO;

        BigDecimal expensesBank = adminProfileId != null
                ? expenseRepository.sumAmountByAdminProfileIdAndPaymentMode(adminProfileId, ExpensePaymentMode.BANK) : BigDecimal.ZERO;
        if (expensesBank == null) expensesBank = BigDecimal.ZERO;

        BigDecimal cashBalance = cashCollected.subtract(expensesCash);
        BigDecimal bankBalance = advanceReceived.subtract(expensesBank);
        BigDecimal netProfit = netRevenue.subtract(totalExpenses);

        return ServiceFinancialSummary.builder()
                .grossRevenue(grossRevenue)
                .cancelledRevenue(cancelledRevenue)
                .refundedRevenue(refundedRevenue)
                .netRevenue(netRevenue)
                .advancePending(advancePending)
                .advanceReceived(advanceReceived)
                .cashCollected(cashCollected)
                .cashBalance(cashBalance)
                .bankBalance(bankBalance)
                .netProfit(netProfit)
                .build();
    }

    public CashBankSummary getCashBankSummary(Long serviceId) {
        BigDecimal cashCollected = bookingRepository.calculateCashCollected(serviceId);
        if (cashCollected == null) cashCollected = BigDecimal.ZERO;

        BigDecimal advanceReceived = bookingRepository.calculateAdvanceReceived(serviceId);
        if (advanceReceived == null) advanceReceived = BigDecimal.ZERO;

        Service service = serviceRepository.findById(serviceId).orElse(null);
        Long adminProfileId = service != null ? service.getCreatedBy().getId() : null;

        BigDecimal expensesCash = adminProfileId != null
                ? expenseRepository.sumAmountByAdminProfileIdAndPaymentMode(adminProfileId, ExpensePaymentMode.CASH) : BigDecimal.ZERO;
        if (expensesCash == null) expensesCash = BigDecimal.ZERO;

        BigDecimal expensesBank = adminProfileId != null
                ? expenseRepository.sumAmountByAdminProfileIdAndPaymentMode(adminProfileId, ExpensePaymentMode.BANK) : BigDecimal.ZERO;
        if (expensesBank == null) expensesBank = BigDecimal.ZERO;

        return CashBankSummary.builder()
                .cashCollected(cashCollected)
                .expensesCash(expensesCash)
                .cashBalance(cashCollected.subtract(expensesCash))
                .advanceReceived(advanceReceived)
                .expensesBank(expensesBank)
                .bankBalance(advanceReceived.subtract(expensesBank))
                .build();
    }

    @Transactional
    public ExpenseResponse addExpense(Long serviceId, ExpenseRequest request) {
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        AdminProfile adminProfile = service.getCreatedBy();

        Expense expense = new Expense();
        expense.setAdminProfile(adminProfile);
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setPaymentMode(request.getPaymentMode());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setBillUrl(request.getBillUrl());

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    public List<ExpenseResponse> getExpenses(Long serviceId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        Service service = serviceRepository.findById(serviceId).orElse(null);
        Long adminProfileId = service != null ? service.getCreatedBy().getId() : null;

        if (adminProfileId == null) return List.of();

        return expenseRepository.findByAdminProfileIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                        adminProfileId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .serviceId(null) // Expenses are now at admin level
                .category(expense.getCategory())
                .amount(expense.getAmount())
                .paymentMode(expense.getPaymentMode())
                .description(expense.getDescription())
                .expenseDate(expense.getExpenseDate())
                .billUrl(expense.getBillUrl())
                .createdBy(expense.getCreatedBy())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
