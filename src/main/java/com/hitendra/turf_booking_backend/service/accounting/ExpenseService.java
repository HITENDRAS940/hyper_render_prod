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
     * 1. Validate service belongs to current admin
     * 2. Validate category exists (can be from any admin - sharing allowed)
     * 3. Create expense record for the service
     * 4. Record debit in cash ledger
     *
     * NOTE: Expenses are service-specific, so they're automatically admin-specific
     * through the service relationship. Categories can be used across admins.
     *
     * @param request Expense details
     * @return Created expense
     */
    @Transactional
    public Expense createExpense(CreateExpenseRequest request) {
        log.info("Creating expense for service {}: {} - {}",
            request.getServiceId(), request.getAmount(), request.getDescription());

        // Get current admin
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();

        // Validate service
        Service service = serviceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new BookingException("Service not found: " + request.getServiceId()));

        // Verify service belongs to current admin
        if (!service.getCreatedBy().getId().equals(currentAdminId)) {
            throw new BookingException("Access denied: This service belongs to another admin");
        }

        // Validate category exists (category can be from any admin)
        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new BookingException("Expense category not found: " + request.getCategoryId()));

        // Get current user
        String currentUser = authUtil.getCurrentUser().getPhone();

        // Create expense - it's automatically admin-specific because service belongs to admin
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

        log.info("Expense created with ID: {} for service {} (admin {})",
                 savedExpense.getId(), service.getId(), currentAdminId);

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
     * Get all expenses for a service (validates ownership).
     */
    public List<Expense> getExpensesByService(Long serviceId) {
        // Validate service ownership
        Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        if (!service.getCreatedBy().getId().equals(currentAdminId)) {
            throw new BookingException("Access denied: This service belongs to another admin");
        }

        return expenseRepository.findByServiceIdOrderByExpenseDateDesc(serviceId);
    }

    /**
     * Get expenses for a service within a date range (validates ownership).
     */
    public List<Expense> getExpensesByServiceAndDateRange(
        Long serviceId, LocalDate startDate, LocalDate endDate) {

        // Validate service ownership
        Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        if (!service.getCreatedBy().getId().equals(currentAdminId)) {
            throw new BookingException("Access denied: This service belongs to another admin");
        }

        return expenseRepository.findByServiceIdAndExpenseDateBetweenOrderByExpenseDateDesc(
            serviceId, startDate, endDate);
    }

    /**
     * Get total expenses for a service in a date range (validates ownership).
     */
    public Double getTotalExpenses(Long serviceId, LocalDate startDate, LocalDate endDate) {
        // Validate service ownership
        Service service = serviceRepository.findById(serviceId)
            .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        if (!service.getCreatedBy().getId().equals(currentAdminId)) {
            throw new BookingException("Access denied: This service belongs to another admin");
        }

        return expenseRepository.getTotalExpensesByServiceAndDateRange(serviceId, startDate, endDate);
    }

    /**
     * Get expense breakdown by category.
     */
    public List<Object[]> getExpenseBreakdown(Long serviceId, LocalDate startDate, LocalDate endDate) {
        return expenseRepository.getExpenseBreakdownByCategory(serviceId, startDate, endDate);
    }
}

