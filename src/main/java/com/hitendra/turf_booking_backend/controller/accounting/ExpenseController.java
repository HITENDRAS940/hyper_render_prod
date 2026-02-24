package com.hitendra.turf_booking_backend.controller.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.CreateExpenseRequest;
import com.hitendra.turf_booking_backend.dto.accounting.ExpenseDto;
import com.hitendra.turf_booking_backend.entity.accounting.Expense;
import com.hitendra.turf_booking_backend.service.accounting.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/accounting/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Manage your expenses (Admin-specific)")
@PreAuthorize("hasRole('ADMIN')")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @Operation(summary = "Record an expense",
               description = """
                   Create a new expense record for the current admin and debit from ledger.
                   - Category must belong to current admin
                   - Expense is linked directly to admin, not to a specific service
                   - Automatically records in admin ledger
                   """)
    public ResponseEntity<ExpenseDto> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        Expense expense = expenseService.createExpense(request);
        return ResponseEntity.ok(mapToDto(expense));
    }

    @GetMapping
    @Operation(summary = "Get my expenses",
               description = "Get all expenses for the current admin.")
    public ResponseEntity<List<ExpenseDto>> getMyExpenses() {
        List<Expense> expenses = expenseService.getExpensesForCurrentAdmin();
        return ResponseEntity.ok(expenses.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    @GetMapping("/range")
    @Operation(summary = "Get expenses by date range",
               description = "Get expenses for the current admin within a date range.")
    public ResponseEntity<List<ExpenseDto>> getExpensesByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<Expense> expenses = expenseService.getExpensesByDateRange(startDate, endDate);
        return ResponseEntity.ok(expenses.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    @GetMapping("/total")
    @Operation(summary = "Get total expenses",
               description = "Get total expense amount for the current admin in a date range.")
    public ResponseEntity<Map<String, Double>> getTotalExpenses(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Double total = expenseService.getTotalExpenses(startDate, endDate);
        Map<String, Double> response = new HashMap<>();
        response.put("totalExpenses", total);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/breakdown")
    @Operation(summary = "Get expense breakdown",
               description = "Get expense breakdown by category for the current admin.")
    public ResponseEntity<Map<String, Double>> getExpenseBreakdown(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<Object[]> breakdownData = expenseService.getExpenseBreakdown(startDate, endDate);
        Map<String, Double> breakdown = new HashMap<>();
        for (Object[] row : breakdownData) {
            String category = (String) row[0];
            Double amount = ((Number) row[1]).doubleValue();
            breakdown.put(category, amount);
        }
        return ResponseEntity.ok(breakdown);
    }

    private ExpenseDto mapToDto(Expense expense) {
        return ExpenseDto.builder()
            .id(expense.getId())
            .adminId(expense.getAdminProfile().getId())
            .adminName(expense.getAdminProfile().getBusinessName() != null
                ? expense.getAdminProfile().getBusinessName()
                : expense.getAdminProfile().getUser() != null
                    ? expense.getAdminProfile().getUser().getName()
                    : null)
            .categoryId(null)
            .categoryName(expense.getCategory())
            .categoryType("EXPENSE")
            .description(expense.getDescription())
            .amount(expense.getAmount() != null ? expense.getAmount().doubleValue() : 0.0)
            .paymentMode(expense.getPaymentMode())
            .expenseDate(expense.getExpenseDate())
            .referenceNumber(expense.getBillUrl())
            .createdBy(String.valueOf(expense.getCreatedBy()))
            .createdAt(expense.getCreatedAt())
            .build();
    }
}
