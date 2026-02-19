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

import java.util.HashMap;
import java.util.Map;

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
@Tag(name = "Manual Adjustments", description = "Manual cash/bank adjustments for admins")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class ManualAdjustmentController {

    private final ManualAdjustmentService adjustmentService;

    /**
     * Record a manual adjustment (credit or debit) for a service.
     *
     * Use cases:
     * - Cash deposit to bank: DEBIT with CASH mode, description "Deposited to bank"
     * - Opening balance: CREDIT with CASH mode, description "Opening balance"
     * - Correction: CREDIT or DEBIT with appropriate mode and description
     *
     * @param serviceId The service ID
     * @param request The adjustment details
     * @return Response with ledger entry details
     */
    @PostMapping("/service/{serviceId}")
    @Operation(
            summary = "Record manual adjustment",
            description = "Record a manual credit (money in) or debit (money out) adjustment for a service. " +
                    "Use for cash deposits, corrections, or initial balance setup. " +
                    "Only the service owner can make adjustments."
    )
    public ResponseEntity<ManualAdjustmentResponseDto> recordAdjustment(
            @PathVariable Long serviceId,
            @Valid @RequestBody ManualAdjustmentRequestDto request) {

        ManualAdjustmentResponseDto response = adjustmentService.recordManualAdjustment(serviceId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current balance for a service.
     *
     * @param serviceId The service ID
     * @return Current balance
     */
    @GetMapping("/service/{serviceId}/balance")
    @Operation(
            summary = "Get current balance",
            description = "Get the current ledger balance for a service. " +
                    "Only the service owner can view the balance."
    )
    public ResponseEntity<Map<String, Object>> getCurrentBalance(@PathVariable Long serviceId) {
        Double balance = adjustmentService.getCurrentBalance(serviceId);

        Map<String, Object> response = new HashMap<>();
        response.put("serviceId", serviceId);
        response.put("currentBalance", balance);
        response.put("currency", "INR");

        return ResponseEntity.ok(response);
    }
}

