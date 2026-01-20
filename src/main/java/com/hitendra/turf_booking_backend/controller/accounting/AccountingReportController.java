package com.hitendra.turf_booking_backend.controller.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.CashLedgerDto;
import com.hitendra.turf_booking_backend.dto.accounting.ProfitLossReportDto;
import com.hitendra.turf_booking_backend.entity.accounting.CashLedger;
import com.hitendra.turf_booking_backend.repository.accounting.CashLedgerRepository;
import com.hitendra.turf_booking_backend.service.accounting.AccountingReportService;
import com.hitendra.turf_booking_backend.service.accounting.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/accounting/reports")
@RequiredArgsConstructor
@Tag(name = "Accounting Reports", description = "Financial reports and ledger (Admin only)")
@PreAuthorize("hasRole('ADMIN')")
public class AccountingReportController {

    private final AccountingReportService reportService;
    private final LedgerService ledgerService;
    private final CashLedgerRepository ledgerRepository;

    @GetMapping("/profit-loss")
    @Operation(summary = "Get P&L report", description = "Generate Profit & Loss statement for a service")
    public ResponseEntity<ProfitLossReportDto> getProfitLossReport(
        @RequestParam Long serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        ProfitLossReportDto report = reportService.generateProfitLossReport(serviceId, startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/ledger/service/{serviceId}")
    @Operation(summary = "Get ledger entries", description = "Get all cash ledger entries for a service")
    public ResponseEntity<List<CashLedgerDto>> getLedgerEntries(@PathVariable Long serviceId) {
        List<CashLedger> entries = ledgerRepository.findByServiceIdOrderByCreatedAtDesc(serviceId);
        return ResponseEntity.ok(entries.stream().map(this::mapLedgerToDto).collect(Collectors.toList()));
    }

    @GetMapping("/ledger/service/{serviceId}/range")
    @Operation(summary = "Get ledger by date range", description = "Get ledger entries for a service within a date range")
    public ResponseEntity<List<CashLedgerDto>> getLedgerByDateRange(
        @PathVariable Long serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<CashLedger> entries = ledgerRepository.findByServiceIdAndDateRange(serviceId, startInstant, endInstant);
        return ResponseEntity.ok(entries.stream().map(this::mapLedgerToDto).collect(Collectors.toList()));
    }

    @GetMapping("/balance/{serviceId}")
    @Operation(summary = "Get current balance", description = "Get current cash balance for a service")
    public ResponseEntity<Map<String, Double>> getCurrentBalance(@PathVariable Long serviceId) {
        Double balance = ledgerService.getCurrentBalance(serviceId);
        Map<String, Double> response = new HashMap<>();
        response.put("currentBalance", balance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Get cash flow summary", description = "Get cash in vs cash out for a service in a date range")
    public ResponseEntity<Map<String, Object>> getCashFlowSummary(
        @RequestParam Long serviceId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Double totalCashIn = ledgerRepository.getTotalCreditsByServiceAndDateRange(serviceId, startInstant, endInstant);
        Double totalCashOut = ledgerRepository.getTotalDebitsByServiceAndDateRange(serviceId, startInstant, endInstant);
        Double currentBalance = ledgerService.getCurrentBalance(serviceId);

        if (totalCashIn == null) totalCashIn = 0.0;
        if (totalCashOut == null) totalCashOut = 0.0;

        Double netCashFlow = totalCashIn - totalCashOut;

        Map<String, Object> response = new HashMap<>();
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("totalCashIn", totalCashIn);
        response.put("totalCashOut", totalCashOut);
        response.put("netCashFlow", netCashFlow);
        response.put("currentBalance", currentBalance);

        return ResponseEntity.ok(response);
    }

    private CashLedgerDto mapLedgerToDto(CashLedger ledger) {
        return CashLedgerDto.builder()
            .id(ledger.getId())
            .serviceId(ledger.getService().getId())
            .serviceName(ledger.getService().getName())
            .source(ledger.getSource())
            .referenceType(ledger.getReferenceType())
            .referenceId(ledger.getReferenceId())
            .creditAmount(ledger.getCreditAmount())
            .debitAmount(ledger.getDebitAmount())
            .balanceAfter(ledger.getBalanceAfter())
            .paymentMode(ledger.getPaymentMode())
            .description(ledger.getDescription())
            .recordedBy(ledger.getRecordedBy())
            .createdAt(ledger.getCreatedAt())
            .build();
    }
}

