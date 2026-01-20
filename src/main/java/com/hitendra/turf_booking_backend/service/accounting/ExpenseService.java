package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.CreateExpenseRequest;
import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.entity.accounting.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseCategoryRepository;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing expenses (outgoing money).
 *
 * FLOW:
 * 1. Create expense record
 * 2. Record in cash ledger (debit)
 *
 * Examples: Electricity bill, maintenance, salaries
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final LedgerService ledgerService;
    private final AuthUtil authUtil;

    /**
     * Create a new expense and record in ledger.
     *
     * TRANSACTION FLOW:
     * 1. Validate service and category
     * 2. Create expense record
     * 3. Record debit in cash ledger
     *
     * @param request Expense details
     * @return Created expense
     */
    @Transactional
    public Expense createExpense(CreateExpenseRequest request) {
        log.info("Creating expense for service {}: {} - {}",
            request.getServiceId(), request.getAmount(), request.getDescription());

        // Validate service
        Service service = serviceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new BookingException("Service not found: " + request.getServiceId()));

        // Validate category
        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new BookingException("Expense category not found: " + request.getCategoryId()));

        // Get current user
        String currentUser = authUtil.getCurrentUser().getPhone();

        // Create expense
        Expense expense = Expense.builder()
            .service(service)
            .category(category)
            .description(request.getDescription())
            .amount(request.getAmount())
            .paymentMode(request.getPaymentMode())
            .expenseDate(request.getExpenseDate())
            .referenceNumber(request.getReferenceNumber())
            .createdBy(currentUser)
            .build();

        Expense savedExpense = expenseRepository.save(expense);

        log.info("Expense created with ID: {}", savedExpense.getId());

        // Record in ledger (DEBIT)
        ledgerService.recordDebit(
            service,
            LedgerSource.EXPENSE,
            ReferenceType.EXPENSE,
            savedExpense.getId(),
            savedExpense.getAmount(),
            savedExpense.getPaymentMode(),
            "Expense: " + category.getName() + " - " + savedExpense.getDescription(),
            currentUser
        );

        log.info("Expense recorded in ledger successfully");

        return savedExpense;
    }

    /**
     * Get all expenses for a service.
     */
    public List<Expense> getExpensesByService(Long serviceId) {
        return expenseRepository.findByServiceIdOrderByExpenseDateDesc(serviceId);
    }

    /**
     * Get expenses for a service within a date range.
     */
    public List<Expense> getExpensesByServiceAndDateRange(
        Long serviceId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.findByServiceIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            serviceId, startDate, endDate);
    }

    /**
     * Get total expenses for a service in a date range.
     */
    public Double getTotalExpenses(Long serviceId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.getTotalExpensesByServiceAndDateRange(serviceId, startDate, endDate);
    }

    /**
     * Get expense breakdown by category.
     */
    public List<Object[]> getExpenseBreakdown(Long serviceId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.getExpenseBreakdownByCategory(serviceId, startDate, endDate);
    }
}

