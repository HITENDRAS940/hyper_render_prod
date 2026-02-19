package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.*;
import com.hitendra.turf_booking_backend.entity.accounting.Expense;
import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.RefundRepository;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseRepository;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceFinancialService {

    private final BookingRepository bookingRepository;
    private final RefundRepository refundRepository;
    private final ExpenseRepository expenseRepository;
    private final ServiceRepository serviceRepository; // For service validation

    public ServiceFinancialSummary getDashboardSummary(Long serviceId, LocalDate startDate, LocalDate endDate) {

        // Use default date range if not provided (e.g., current month)
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        // 1. Booking Metrics (Gross, Cancelled)
        Double grossRevDouble = bookingRepository.calculateGrossRevenue(serviceId, startDate, endDate);
        BigDecimal grossRevenue = grossRevDouble != null ? BigDecimal.valueOf(grossRevDouble) : BigDecimal.ZERO;

        Double cancelledRevDouble = bookingRepository.calculateCancelledRevenue(serviceId);
        BigDecimal cancelledRevenue = cancelledRevDouble != null ? BigDecimal.valueOf(cancelledRevDouble) : BigDecimal.ZERO;

        // 2. Refund Metrics
        BigDecimal refundedRevenue = refundRepository.calculateRefundedRevenue(serviceId, startDate, endDate);
        if (refundedRevenue == null) refundedRevenue = BigDecimal.ZERO;

        // 3. Net Revenue
        BigDecimal netRevenue = grossRevenue.subtract(refundedRevenue);

        // 4. Advance Metrics (Platform held vs Transferred)
        BigDecimal advancePending = bookingRepository.calculateAdvancePending(serviceId);
        if (advancePending == null) advancePending = BigDecimal.ZERO;

        BigDecimal advanceReceived = bookingRepository.calculateAdvanceReceived(serviceId);
        if (advanceReceived == null) advanceReceived = BigDecimal.ZERO;

        // 5. Cash Collection (Service side)
        BigDecimal cashCollected = bookingRepository.calculateCashCollected(serviceId);
        if (cashCollected == null) cashCollected = BigDecimal.ZERO;

        // 6. Expense Metrics
        BigDecimal totalExpenses = expenseRepository.sumAmountByServiceId(serviceId);
        if (totalExpenses == null) totalExpenses = BigDecimal.ZERO;

        BigDecimal expensesCash = expenseRepository.sumAmountByServiceIdAndPaymentMode(serviceId, PaymentMode.CASH);
        if (expensesCash == null) expensesCash = BigDecimal.ZERO;

        BigDecimal expensesBank = expenseRepository.sumAmountByServiceIdAndPaymentMode(serviceId, PaymentMode.BANK_TRANSFER);
        if (expensesBank == null) expensesBank = BigDecimal.ZERO;

        // 7. Balances
        BigDecimal cashBalance = cashCollected.subtract(expensesCash);
        // Bank Balance = Advance Received - Bank Expenses
        // Note: This is simplified. Real bank balance would include other transactions.
        // Assuming Advance Received is the only inflow to bank tracked here.
        BigDecimal bankBalance = advanceReceived.subtract(expensesBank);

        // 8. Net Profit
        // Net Profit = Net Revenue - Total Expenses
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

        BigDecimal expensesCash = expenseRepository.sumAmountByServiceIdAndPaymentMode(serviceId, PaymentMode.CASH);
        if (expensesCash == null) expensesCash = BigDecimal.ZERO;

        BigDecimal expensesBank = expenseRepository.sumAmountByServiceIdAndPaymentMode(serviceId, PaymentMode.BANK_TRANSFER);
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
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        Expense expense = new Expense();
        expense.setService(service);
        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setPaymentMode(request.getPaymentMode());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setBillUrl(request.getBillUrl());
        // Set createdBy from security context if available, for now omitting or passing via DTO if needed

        Expense saved = expenseRepository.save(expense);

        return mapToResponse(saved);
    }

    public List<ExpenseResponse> getExpenses(Long serviceId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        return expenseRepository.findByServiceIdAndExpenseDateBetweenOrderByExpenseDateDesc(serviceId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .serviceId(expense.getService().getId())
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
