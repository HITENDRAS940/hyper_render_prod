package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.CreateExpenseRequest;
import com.hitendra.turf_booking_backend.entity.AdminProfile;
import com.hitendra.turf_booking_backend.entity.accounting.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseCategoryRepository;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing expenses (outgoing money) at the admin level.
 *
 * FLOW:
 * 1. Validate paymentMode is CASH or BANK
 * 2. Check admin's balance (cash or bank) >= expense amount
 * 3. Deduct the amount from the corresponding balance
 * 4. Save expense record
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final AuthUtil authUtil;
    private final EntityManager entityManager;

    @Transactional
    public Expense createExpense(CreateExpenseRequest request) {
        // Lock the admin row to prevent concurrent balance issues
        AdminProfile currentAdmin = entityManager.find(
                AdminProfile.class,
                authUtil.getCurrentAdminProfile().getId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        ExpensePaymentMode mode = request.getPaymentMode();

        log.info("Creating {} expense for admin {}: amount={}, desc={}",
                mode, currentAdmin.getId(), amount, request.getDescription());

        // ── Balance check ────────────────────────────────────────────────────
        if (mode == ExpensePaymentMode.CASH) {
            BigDecimal cashBal = currentAdmin.getCashBalance() != null
                    ? currentAdmin.getCashBalance() : BigDecimal.ZERO;
            if (cashBal.compareTo(amount) < 0) {
                throw new BookingException(
                        String.format("Insufficient cash balance. Available: ₹%s, Required: ₹%s",
                                cashBal, amount));
            }
            currentAdmin.setCashBalance(cashBal.subtract(amount));
        } else { // BANK
            BigDecimal bankBal = currentAdmin.getBankBalance() != null
                    ? currentAdmin.getBankBalance() : BigDecimal.ZERO;
            if (bankBal.compareTo(amount) < 0) {
                throw new BookingException(
                        String.format("Insufficient bank balance. Available: ₹%s, Required: ₹%s",
                                bankBal, amount));
            }
            currentAdmin.setBankBalance(bankBal.subtract(amount));
        }

        // Balance update will be flushed automatically — currentAdmin is a managed entity

        // ── Validate category ────────────────────────────────────────────────
        ExpenseCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BookingException("Expense category not found: " + request.getCategoryId()));

        if (!category.getAdminProfile().getId().equals(currentAdmin.getId())) {
            throw new BookingException("Access denied: This category belongs to another admin");
        }

        // ── Save expense record ──────────────────────────────────────────────
        Expense expense = Expense.builder()
                .adminProfile(currentAdmin)
                .category(category.getName())
                .description(request.getDescription())
                .amount(amount)
                .paymentMode(mode)
                .expenseDate(request.getExpenseDate())
                .billUrl(request.getReferenceNumber())
                .createdBy(authUtil.getCurrentUserId())
                .build();

        Expense saved = expenseRepository.save(expense);
        log.info("Expense ID={} recorded. {} balance deducted by ₹{}. New {} balance: ₹{}",
                saved.getId(), mode, amount, mode,
                mode == ExpensePaymentMode.CASH
                        ? currentAdmin.getCashBalance()
                        : currentAdmin.getBankBalance());
        return saved;
    }

    /**
     * Get all expenses for the current admin.
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesForCurrentAdmin() {
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        return expenseRepository.findByAdminProfileIdWithRelationships(currentAdminId);
    }

    /**
     * Get expenses for the current admin within a date range.
     */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        return expenseRepository.findByAdminProfileIdAndDateRangeWithRelationships(
            currentAdminId, startDate, endDate);
    }

    /**
     * Get total expenses for the current admin in a date range.
     */
    @Transactional(readOnly = true)
    public Double getTotalExpenses(LocalDate startDate, LocalDate endDate) {
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        return expenseRepository.getTotalExpensesByAdminProfileIdAndDateRange(currentAdminId, startDate, endDate);
    }

    /**
     * Get expense breakdown by category for the current admin.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getExpenseBreakdown(LocalDate startDate, LocalDate endDate) {
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        return expenseRepository.getExpenseBreakdownByCategory(currentAdminId, startDate, endDate);
    }

    // ==================== LEGACY SERVICE-BASED METHODS ====================

    /** @deprecated Use getExpensesForCurrentAdmin() instead. */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByService(Long serviceId) {
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        return expenseRepository.findByAdminProfileIdWithRelationships(currentAdminId);
    }

    /** @deprecated Use getExpensesByDateRange() instead. */
    @Transactional(readOnly = true)
    public List<Expense> getExpensesByServiceAndDateRange(Long serviceId, LocalDate startDate, LocalDate endDate) {
        Long currentAdminId = authUtil.getCurrentAdminProfile().getId();
        return expenseRepository.findByAdminProfileIdAndDateRangeWithRelationships(currentAdminId, startDate, endDate);
    }
}

