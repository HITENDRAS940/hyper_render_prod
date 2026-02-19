package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.CashBankSummary;
import com.hitendra.turf_booking_backend.dto.ExpenseRequest;
import com.hitendra.turf_booking_backend.dto.ExpenseResponse;
import com.hitendra.turf_booking_backend.dto.ServiceFinancialSummary;
import com.hitendra.turf_booking_backend.service.ServiceFinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/service/financial")
@RequiredArgsConstructor
public class ServiceFinancialController {

    private final ServiceFinancialService financialService;

    @GetMapping("/{serviceId}/dashboard")
    @PreAuthorize("hasRole('PLATFORM_ADMIN') or @securityService.isServiceAdmin(#serviceId)")
    public ResponseEntity<ServiceFinancialSummary> getDashboardSummary(
            @PathVariable Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(financialService.getDashboardSummary(serviceId, startDate, endDate));
    }

    @GetMapping("/{serviceId}/cash-bank-summary")
    @PreAuthorize("hasRole('PLATFORM_ADMIN') or @securityService.isServiceAdmin(#serviceId)")
    public ResponseEntity<CashBankSummary> getCashBankSummary(@PathVariable Long serviceId) {
        return ResponseEntity.ok(financialService.getCashBankSummary(serviceId));
    }

    @PostMapping("/{serviceId}/expenses")
    @PreAuthorize("hasRole('PLATFORM_ADMIN') or @securityService.isServiceAdmin(#serviceId)")
    public ResponseEntity<ExpenseResponse> addExpense(
            @PathVariable Long serviceId,
            @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(financialService.addExpense(serviceId, request));
    }

    @GetMapping("/{serviceId}/expenses")
    @PreAuthorize("hasRole('PLATFORM_ADMIN') or @securityService.isServiceAdmin(#serviceId)")
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @PathVariable Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(financialService.getExpenses(serviceId, startDate, endDate));
    }
}

