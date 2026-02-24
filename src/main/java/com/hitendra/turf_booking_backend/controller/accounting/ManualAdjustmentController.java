package com.hitendra.turf_booking_backend.controller.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentRequestDto;
import com.hitendra.turf_booking_backend.dto.accounting.ManualAdjustmentResponseDto;
import com.hitendra.turf_booking_backend.service.accounting.ManualAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for manual cash and bank adjustments by admins.
 *
 * Features:
 * - Record manual adjustments (credit/debit)
 * - Cash deposits to bank
 * - Balance corrections
 * - Initial balance setup
 * - Get current balance
 */
@RestController
@RequestMapping("/api/admin/accounting/adjustments")
@RequiredArgsConstructor
@Tag(name = "Manual Adjustments", description = "Manual cash/bank adjustments on admin balance")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class ManualAdjustmentController {

    private final ManualAdjustmentService adjustmentService;

    /**
     * Record a manual adjustment (credit or debit) for admin's cash or bank balance.
     *
     * Use cases:
     * - Cash deposit to bank: DEBIT with CASH mode, description "Deposited to bank"
     * - Opening balance: CREDIT with CASH mode, description "Opening balance"
     * - Correction: CREDIT or DEBIT with appropriate mode and description
     *
     * @param request The adjustment details
     * @return Response with ledger entry details
     */
    @PostMapping
    @Operation(
            summary = "Record manual adjustment",
            description = """
                Record a manual CREDIT (money in) or DEBIT (money out) on the admin's cash or bank balance.
                
                **paymentMode** must be `CASH` or `BANK`.
                
                **Examples:**
                - Opening cash balance: `{"type":"CREDIT","paymentMode":"CASH","amount":5000,"description":"Opening balance"}`
                - Cash deposit to bank: Two entries — DEBIT CASH + CREDIT BANK for the same amount
                - Correction debit: `{"type":"DEBIT","paymentMode":"BANK","amount":200,"description":"Bank charge correction"}`
                
                **Rules:**
                - DEBIT requires sufficient balance (balance >= amount)
                - All adjustments are recorded in the admin ledger for full audit trail
                """
    )
    public ResponseEntity<ManualAdjustmentResponseDto> recordAdjustment(
            @Valid @RequestBody ManualAdjustmentRequestDto request) {
        return ResponseEntity.ok(adjustmentService.recordManualAdjustment(request));
    }

    /**
     * Get current cash and bank balances for the admin.
     *
     * @return Current cash and bank balances
     */
    @GetMapping("/balance")
    @Operation(
            summary = "Get current cash & bank balances",
            description = "Returns the current cash balance (previousBalance) and bank balance (newBalance) for the current admin."
    )
    public ResponseEntity<ManualAdjustmentResponseDto> getCurrentBalance() {
        return ResponseEntity.ok(adjustmentService.getCurrentBalances());
    }
}
