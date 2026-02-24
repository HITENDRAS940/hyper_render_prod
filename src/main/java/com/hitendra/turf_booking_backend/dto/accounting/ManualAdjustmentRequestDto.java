package com.hitendra.turf_booking_backend.dto.accounting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for manual cash/bank adjustments by admin.
 * Used for:
 * - Cash deposits to bank
 * - Manual corrections
 * - Initial balance setup
 * - Reconciliation adjustments
 *
 * paymentMode must be CASH or BANK only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAdjustmentRequestDto {

    @NotNull(message = "Adjustment type is required (CREDIT or DEBIT)")
    private AdjustmentType type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    /** Which balance to adjust — CASH or BANK only */
    @NotNull(message = "Payment mode is required (CASH or BANK)")
    private AdjustmentMode paymentMode;

    @NotBlank(message = "Description is required")
    private String description;

    /** Optional reference number (e.g., bank transaction ID, receipt number) */
    private String referenceNumber;

    public enum AdjustmentType {
        CREDIT,  // Money IN  – increases balance
        DEBIT    // Money OUT – decreases balance
    }

    public enum AdjustmentMode {
        CASH,   // Adjust admin's cash balance
        BANK    // Adjust admin's bank balance
    }
}
