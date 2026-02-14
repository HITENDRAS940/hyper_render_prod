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
                   Create a new expense record for your service and debit from cash ledger.
                   - Service must belong to current admin
                   - Category must belong to current admin
                   - Automatically records in ledger
                   """)
    public ResponseEntity<ExpenseDto> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        Expense expense = expenseService.createExpense(request);
        return ResponseEntity.ok(mapToDto(expense));
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get expenses by service",
               description = "Get all expenses for your service. Only shows expenses for services you own.")
    public ResponseEntity<List<ExpenseDto>> getExpensesByService(@PathVariable Long serviceId) {
        List<Expense> expenses = expenseService.getExpensesByService(serviceId);
        return ResponseEntity.ok(expenses.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    @GetMapping("/service/{serviceId}/range")
    @Operation(summary = "Get expenses by date range",
               description = "Get expenses for your service within a date range. Only shows expenses for services you own.")
    public ResponseEntity<List<ExpenseDto>> getExpensesByDateRange(
        @PathVariable Long serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<Expense> expenses = expenseService.getExpensesByServiceAndDateRange(serviceId, startDate, endDate);
        return ResponseEntity.ok(expenses.stream().map(this::mapToDto).collect(Collectors.toList()));
    }

    @GetMapping("/service/{serviceId}/total")
    @Operation(summary = "Get total expenses",
               description = "Get total expense amount for your service in a date range. Only accessible for services you own.")
    public ResponseEntity<Map<String, Double>> getTotalExpenses(
        @PathVariable Long serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Double total = expenseService.getTotalExpenses(serviceId, startDate, endDate);
        Map<String, Double> response = new HashMap<>();
        response.put("totalExpenses", total);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/service/{serviceId}/breakdown")
    @Operation(summary = "Get expense breakdown",
               description = "Get expense breakdown by category for your service. Shows how much was spent in each category.")
    public ResponseEntity<Map<String, Double>> getExpenseBreakdown(
        @PathVariable Long serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<Object[]> breakdownData = expenseService.getExpenseBreakdown(serviceId, startDate, endDate);
        Map<String, Double> breakdown = new HashMap<>();
        for (Object[] row : breakdownData) {
            String category = (String) row[0];
            Double amount = (Double) row[1];
            breakdown.put(category, amount);
        }
        return ResponseEntity.ok(breakdown);
    }

    private ExpenseDto mapToDto(Expense expense) {
        return ExpenseDto.builder()
            .id(expense.getId())
            .serviceId(expense.getService().getId())
            .serviceName(expense.getService().getName())
            .categoryId(expense.getCategory().getId())
            .categoryName(expense.getCategory().getName())
            .categoryType(expense.getCategory().getType().name())
            .description(expense.getDescription())
            .amount(expense.getAmount())
            .paymentMode(expense.getPaymentMode())
            .expenseDate(expense.getExpenseDate())
            .referenceNumber(expense.getReferenceNumber())
            .createdBy(expense.getCreatedBy())
            .createdAt(expense.getCreatedAt())
            .build();
    }
}

