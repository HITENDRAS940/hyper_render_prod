package com.hitendra.turf_booking_backend.dto.financial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for POST /admin/expenses
 *
 * Records a direct admin-level expense (e.g., salary paid in cash, UPI transfer to vendor).
 * Deducts from admin's cash or bank balance and appends a DEBIT entry to admin_ledger.
 *
 * Example: admin paid ₹2000 salary in cash →
 *   paymentMode = CASH, amount = 2000, description = "Salary - John Doe"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminExpenseRequestDto {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    /**
     * CASH or BANK — which sub-ledger to debit.
     */
    @NotBlank(message = "Payment mode is required (CASH or BANK)")
    private String paymentMode;   // "CASH" | "BANK"

    @NotBlank(message = "Description is required")
    private String description;

    /** Optional: category label (e.g., SALARY, MAINTENANCE, UTILITIES) */
    private String category;

    /** Expense date. Defaults to today if null. */
    private LocalDate expenseDate;
}

