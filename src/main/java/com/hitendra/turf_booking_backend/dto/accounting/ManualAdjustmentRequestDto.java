package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAdjustmentRequestDto {

    /**
     * Type of adjustment: CREDIT (money in) or DEBIT (money out)
     */
    @NotNull(message = "Adjustment type is required")
    private AdjustmentType type;

    /**
     * Amount to adjust
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    /**
     * Payment mode for the adjustment
     */
    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    /**
     * Detailed description of the adjustment
     * E.g., "Cash deposit to bank", "Opening balance", "Correction for missing entry"
     */
    @NotBlank(message = "Description is required")
    private String description;

    /**
     * Optional reference number (e.g., bank transaction ID, receipt number)
     */
    private String referenceNumber;

    public enum AdjustmentType {
        CREDIT,  // Money IN - increases balance
        DEBIT    // Money OUT - decreases balance
    }
}

